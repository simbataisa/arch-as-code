#!/usr/bin/env python3
"""
OpenAPI YAML → Markdown Renderer
=================================
Converts an OpenAPI 3.x specification into a comprehensive, GitLab-flavored
Markdown document with embedded MermaidJS and PlantUML diagrams.

Usage:
    python openapi-to-markdown.py openapi.yaml -o api-docs.md

Features:
    - Grouped endpoints by tags
    - Request/response schema tables
    - Mermaid sequence diagrams for key flows
    - PlantUML class diagrams for data models
    - GitLab-compatible rendering
"""

import re
import yaml
import argparse
import sys
import os
from datetime import datetime
from collections import defaultdict


def load_spec(filepath):
    """Load and parse the OpenAPI YAML file."""
    with open(filepath, 'r') as f:
        return yaml.safe_load(f)


def resolve_ref(spec, ref):
    """Resolve a $ref pointer to the actual schema object."""
    if not ref or not ref.startswith('#/'):
        return {}
    parts = ref.lstrip('#/').split('/')
    obj = spec
    for part in parts:
        obj = obj.get(part, {})
    return obj


def get_schema_name(ref_or_schema):
    """Extract schema name from a $ref string."""
    if isinstance(ref_or_schema, str) and ref_or_schema.startswith('#/'):
        return ref_or_schema.split('/')[-1]
    if isinstance(ref_or_schema, dict) and '$ref' in ref_or_schema:
        return ref_or_schema['$ref'].split('/')[-1]
    return None


def resolve_schema(spec, schema):
    """Recursively resolve a schema, handling $ref."""
    if not schema:
        return {}
    if '$ref' in schema:
        resolved = resolve_ref(spec, schema['$ref'])
        return resolved
    return schema


def format_type(spec, schema, depth=0):
    """Format a schema type as a human-readable string."""
    if not schema:
        return 'any'
    if '$ref' in schema:
        name = get_schema_name(schema)
        return f'[{name}](#schema-{name.lower()})' if name else 'object'
    schema_type = schema.get('type', 'any')
    if schema_type == 'array':
        items = schema.get('items', {})
        item_type = format_type(spec, items, depth + 1)
        return f'array\\<{item_type}\\>'
    if schema.get('enum'):
        return f"enum: `{'`, `'.join(str(e) for e in schema['enum'])}`"
    fmt = schema.get('format', '')
    if fmt:
        return f'{schema_type} ({fmt})'
    return schema_type


def render_properties_table(spec, schema, required_fields=None):
    """Render a Markdown table for schema properties."""
    if not schema or 'properties' not in schema:
        return ''
    required_fields = required_fields or schema.get('required', [])
    lines = []
    lines.append('| Field | Type | Required | Description |')
    lines.append('|-------|------|----------|-------------|')
    for prop_name, prop_schema in schema.get('properties', {}).items():
        resolved = resolve_schema(spec, prop_schema)
        ptype = format_type(spec, prop_schema)
        req = '✅' if prop_name in required_fields else ''
        desc = resolved.get('description', prop_schema.get('description', ''))
        # Truncate long descriptions
        if len(desc) > 100:
            desc = desc[:97] + '...'
        lines.append(f'| `{prop_name}` | {ptype} | {req} | {desc} |')
    return '\n'.join(lines)


def render_info(spec):
    """Render the API info section."""
    info = spec.get('info', {})
    lines = []
    lines.append(f'# {info.get("title", "API Documentation")}')
    lines.append('')
    lines.append(f'**Version:** {info.get("version", "N/A")}')
    if info.get('contact'):
        contact = info['contact']
        lines.append(f'**Contact:** {contact.get("name", "")} ({contact.get("email", "")})')
    lines.append(f'**Generated:** {datetime.now().strftime("%Y-%m-%d %H:%M")}')
    lines.append('')
    desc = info.get('description', '')
    if desc:
        lines.append(desc)
        lines.append('')
    return '\n'.join(lines)


def render_servers(spec):
    """Render the servers section."""
    servers = spec.get('servers', [])
    if not servers:
        return ''
    lines = []
    lines.append('## Servers')
    lines.append('')
    lines.append('| URL | Description |')
    lines.append('|-----|-------------|')
    for server in servers:
        lines.append(f'| `{server.get("url", "")}` | {server.get("description", "")} |')
    lines.append('')
    return '\n'.join(lines)


def endpoint_anchor(method, path):
    """Generate a stable, predictable anchor ID for an endpoint heading."""
    slug = re.sub(r'[{}]', '', path.lower())
    slug = re.sub(r'[^a-z0-9/]', '', slug)
    slug = slug.strip('/').replace('/', '-')
    return f'endpoint-{method.lower()}-{slug}'


def render_toc(spec):
    """Render a Table of Contents with level 2 (tags) and level 3 (endpoints/schemas)."""
    tags = spec.get('tags', [])
    paths = spec.get('paths', {})
    schemas = spec.get('components', {}).get('schemas', {})

    # Group endpoints by tag (preserving order)
    grouped = defaultdict(list)
    for path, methods in paths.items():
        for method, operation in methods.items():
            if method in ('get', 'post', 'put', 'delete', 'patch'):
                for tag in operation.get('tags', ['Untagged']):
                    grouped[tag].append((path, method, operation))

    lines = []
    lines.append('## Table of Contents')
    lines.append('')
    lines.append('- [Servers](#servers)')
    lines.append('- [Architecture Overview](#architecture-overview)')
    for tag in tags:
        tag_anchor = tag['name'].lower().replace(' ', '-')
        lines.append(f'- [{tag["name"]}](#{tag_anchor})')
        for path, method, operation in grouped.get(tag['name'], []):
            summary = operation.get('summary', '')
            anchor = endpoint_anchor(method, path)
            label = f'`{method.upper()}` {path}'
            if summary:
                label = f'`{method.upper()}` {path} — {summary}'
            lines.append(f'  - [{label}](#{anchor})')
    lines.append('- [Schemas](#schemas)')
    for name in schemas:
        lines.append(f'  - [{name}](#schema-{name.lower()})')
    lines.append('- [Security](#security)')
    lines.append('')
    return '\n'.join(lines)


def render_architecture_diagram(spec):
    """Generate a MermaidJS architecture overview diagram from servers and tags."""
    servers = spec.get('servers', [])
    tags = spec.get('tags', [])

    lines = []
    lines.append('## Architecture Overview')
    lines.append('')
    lines.append('```mermaid')
    lines.append('flowchart TB')
    lines.append('    CLIENT[API Consumer] ')
    lines.append('')

    # Group tags by server (heuristic based on tag descriptions)
    lines.append('    subgraph Platform["Payment SAGA Platform"]')
    for i, server in enumerate(servers):
        srv_id = f'SRV{i}'
        desc = server.get('description', f'Server {i}')
        port = server.get('url', '').split(':')[-1] if ':' in server.get('url', '') else ''
        lines.append(f'        {srv_id}["{desc}<br/>:{port}"]')
    lines.append('    end')
    lines.append('')

    lines.append('    CLIENT --> SRV0')
    for i in range(1, len(servers)):
        lines.append(f'    SRV0 --> SRV{i}')

    lines.append('```')
    lines.append('')
    return '\n'.join(lines)


def render_endpoint_sequence(path, method, operation, spec):
    """Generate a PlantUML sequence diagram for an endpoint."""
    op_id = operation.get('operationId', 'operation')
    summary = operation.get('summary', '')
    tag = operation.get('tags', ['Service'])[0]

    lines = []
    lines.append(f'```plantuml')
    lines.append(f'@startuml {op_id}')
    lines.append(f'title {summary}')
    lines.append('')
    lines.append('skinparam backgroundColor #FEFEFE')
    lines.append('skinparam sequence {')
    lines.append('    ParticipantBackgroundColor #E3F2FD')
    lines.append('}')
    lines.append('')
    lines.append('actor Client')
    lines.append(f'participant "API Gateway" as GW')
    lines.append(f'participant "{tag}" as SVC')
    lines.append('database "Database" as DB')
    lines.append('')

    # Build request
    lines.append(f'Client -> GW: {method.upper()} {path}')
    lines.append('activate GW')

    # Check security
    security = operation.get('security', [])
    if security:
        lines.append('GW -> GW: Validate Bearer Token')

    lines.append(f'GW -> SVC: Forward request')
    lines.append('activate SVC')

    # Simplified processing
    lines.append('SVC -> DB: Query/Update')
    lines.append('DB --> SVC: Result')

    # Build response
    responses = operation.get('responses', {})
    success_code = next((c for c in responses if c.startswith('2')), '200')
    success_desc = responses.get(success_code, {}).get('description', 'Success')
    lines.append(f'SVC --> GW: {success_code} {success_desc}')
    lines.append('deactivate SVC')
    lines.append(f'GW --> Client: Response')
    lines.append('deactivate GW')

    lines.append('')
    lines.append('@enduml')
    lines.append('```')

    return '\n'.join(lines)


def render_endpoints_by_tag(spec):
    """Render all endpoints grouped by tags."""
    paths = spec.get('paths', {})
    tags_info = {t['name']: t for t in spec.get('tags', [])}

    # Group endpoints by tag
    grouped = defaultdict(list)
    for path, methods in paths.items():
        for method, operation in methods.items():
            if method in ('get', 'post', 'put', 'delete', 'patch'):
                for tag in operation.get('tags', ['Untagged']):
                    grouped[tag].append((path, method, operation))

    lines = []
    lines.append('---')
    lines.append('')

    for tag_name in tags_info:
        endpoints = grouped.get(tag_name, [])
        if not endpoints:
            continue

        tag_info = tags_info.get(tag_name, {})
        lines.append(f'## {tag_name}')
        lines.append('')
        if tag_info.get('description'):
            lines.append(f'> {tag_info["description"]}')
            lines.append('')

        # Summary table for this tag
        lines.append('| Method | Endpoint | Summary |')
        lines.append('|--------|----------|---------|')
        for path, method, operation in endpoints:
            summary = operation.get('summary', '')
            lines.append(f'| `{method.upper()}` | `{path}` | {summary} |')
        lines.append('')

        # Detailed endpoint documentation
        for path, method, operation in endpoints:
            op_id = operation.get('operationId', '')
            summary = operation.get('summary', '')
            desc = operation.get('description', '')

            lines.append(f'<a id="{endpoint_anchor(method, path)}"></a>')
            lines.append(f'### {method.upper()} `{path}`')
            lines.append('')
            if op_id:
                lines.append(f'**Operation ID:** `{op_id}`')
                lines.append('')
            if desc:
                lines.append(desc.strip())
                lines.append('')

            # Security
            security = operation.get('security', [])
            if security:
                scopes = []
                for sec in security:
                    for scheme, scope_list in sec.items():
                        scopes.extend(scope_list)
                if scopes:
                    lines.append(f'**Required Scopes:** `{"`, `".join(scopes)}`')
                else:
                    lines.append('**Authentication:** Bearer Token')
                lines.append('')
            elif security == []:
                lines.append('**Authentication:** None (public)')
                lines.append('')

            # Path Parameters
            params = [p for p in operation.get('parameters', []) if p.get('in') == 'path']
            if params:
                lines.append('**Path Parameters:**')
                lines.append('')
                lines.append('| Parameter | Type | Required | Description |')
                lines.append('|-----------|------|----------|-------------|')
                for p in params:
                    ptype = p.get('schema', {}).get('type', 'string')
                    lines.append(f'| `{p["name"]}` | {ptype} | {p.get("required", False)} | {p.get("description", "")} |')
                lines.append('')

            # Query Parameters
            qparams = [p for p in operation.get('parameters', []) if p.get('in') == 'query']
            if qparams:
                lines.append('**Query Parameters:**')
                lines.append('')
                lines.append('| Parameter | Type | Required | Default | Description |')
                lines.append('|-----------|------|----------|---------|-------------|')
                for p in qparams:
                    pschema = p.get('schema', {})
                    ptype = pschema.get('type', 'string')
                    default = pschema.get('default', '')
                    lines.append(f'| `{p["name"]}` | {ptype} | {p.get("required", False)} | {default} | {p.get("description", "")} |')
                lines.append('')

            # Request Body
            req_body = operation.get('requestBody', {})
            if req_body:
                lines.append('**Request Body:**')
                lines.append('')
                content = req_body.get('content', {})
                for media_type, media_schema in content.items():
                    schema = media_schema.get('schema', {})
                    schema_name = get_schema_name(schema)
                    if schema_name:
                        lines.append(f'Content-Type: `{media_type}` → [{schema_name}](#schema-{schema_name.lower()})')
                    else:
                        resolved = resolve_schema(spec, schema)
                        table = render_properties_table(spec, resolved)
                        if table:
                            lines.append(table)
                    lines.append('')

                    # Example
                    example = media_schema.get('example')
                    if example:
                        lines.append('<details>')
                        lines.append('<summary>Example Request</summary>')
                        lines.append('')
                        lines.append('```json')
                        import json
                        lines.append(json.dumps(example, indent=2))
                        lines.append('```')
                        lines.append('</details>')
                        lines.append('')

            # Responses
            responses = operation.get('responses', {})
            if responses:
                lines.append('**Responses:**')
                lines.append('')
                lines.append('| Code | Description | Schema |')
                lines.append('|------|-------------|--------|')
                for code, response in responses.items():
                    desc = response.get('description', '')
                    content = response.get('content', {})
                    schema_ref = ''
                    for media_type, media_schema in content.items():
                        s = media_schema.get('schema', {})
                        name = get_schema_name(s)
                        if name:
                            schema_ref = f'[{name}](#schema-{name.lower()})'
                        elif s.get('type') == 'object':
                            props = list(s.get('properties', {}).keys())
                            props_str = ", ".join(props[:3])
                            suffix = "..." if len(props) > 3 else ""
                            schema_ref = f'`{{{props_str}{suffix}}}`'
                        elif s.get('type') == 'array':
                            items = s.get('items', {})
                            iname = get_schema_name(items)
                            schema_ref = f'array\\<[{iname}](#schema-{iname.lower()})\\>' if iname else 'array'
                    lines.append(f'| `{code}` | {desc} | {schema_ref} |')
                lines.append('')

            # Generate sequence diagram for POST/PUT endpoints
            if method in ('post', 'put') and len(endpoints) <= 15:
                lines.append(render_endpoint_sequence(path, method, operation, spec))
                lines.append('')

            lines.append('---')
            lines.append('')

    return '\n'.join(lines)


def render_schemas(spec):
    """Render all component schemas."""
    schemas = spec.get('components', {}).get('schemas', {})
    if not schemas:
        return ''

    lines = []
    lines.append('## Schemas')
    lines.append('')

    # Schema index
    lines.append('| Schema | Type | Description |')
    lines.append('|--------|------|-------------|')
    for name, schema in schemas.items():
        stype = schema.get('type', 'object')
        desc = schema.get('description', '')
        if len(desc) > 80:
            desc = desc[:77] + '...'
        lines.append(f'| [{name}](#schema-{name.lower()}) | {stype} | {desc} |')
    lines.append('')

    # PlantUML class diagram for major schemas
    lines.append('### Data Model Overview')
    lines.append('')
    lines.append('```plantuml')
    lines.append('@startuml DataModel')
    lines.append('title API Data Models')
    lines.append('')
    lines.append('skinparam classAttributeIconSize 0')
    lines.append('skinparam class {')
    lines.append('    BackgroundColor #E8F5E9')
    lines.append('    BorderColor #2E7D32')
    lines.append('}')
    lines.append('')

    # Render enum types
    enum_schemas = {n: s for n, s in schemas.items() if s.get('enum')}
    object_schemas = {n: s for n, s in schemas.items()
                      if s.get('type') == 'object' and s.get('properties')}

    for name, schema in enum_schemas.items():
        values = schema.get('enum', [])
        lines.append(f'enum {name} {{')
        for v in values[:8]:  # Limit to 8 values to keep diagram readable
            lines.append(f'    {v}')
        if len(values) > 8:
            lines.append(f'    ... ({len(values) - 8} more)')
        lines.append('}')
        lines.append('')

    # Render top-level request/response objects (limit to keep diagram manageable)
    key_schemas = [n for n in object_schemas
                   if any(k in n.lower() for k in ['request', 'response', 'result', 'order', 'payment'])][:12]

    for name in key_schemas:
        schema = object_schemas[name]
        props = schema.get('properties', {})
        required = schema.get('required', [])
        lines.append(f'class {name} {{')
        for pname, pschema in list(props.items())[:10]:  # Limit properties
            ptype = pschema.get('type', 'object')
            if '$ref' in pschema:
                ptype = pschema['$ref'].split('/')[-1]
            marker = '+' if pname in required else '-'
            lines.append(f'    {marker} {pname}: {ptype}')
        if len(props) > 10:
            lines.append(f'    ... ({len(props) - 10} more fields)')
        lines.append('}')
        lines.append('')

    # Add relationships
    for name in key_schemas:
        schema = object_schemas[name]
        for pname, pschema in schema.get('properties', {}).items():
            ref_name = get_schema_name(pschema)
            if ref_name and ref_name in enum_schemas:
                lines.append(f'{name} --> {ref_name}')
            elif ref_name and ref_name in object_schemas:
                lines.append(f'{name} *-- {ref_name}')
            elif pschema.get('type') == 'array' and pschema.get('items'):
                item_ref = get_schema_name(pschema.get('items', {}))
                if item_ref and (item_ref in object_schemas or item_ref in enum_schemas):
                    lines.append(f'{name} o-- "{" * "}" {item_ref}')

    lines.append('')
    lines.append('@enduml')
    lines.append('```')
    lines.append('')

    # Detailed schema documentation
    for name, schema in schemas.items():
        lines.append(f'### Schema: {name}')
        lines.append(f'<a id="schema-{name.lower()}"></a>')
        lines.append('')

        if schema.get('description'):
            lines.append(schema['description'])
            lines.append('')

        if schema.get('enum'):
            lines.append(f'**Type:** Enum')
            lines.append('')
            lines.append('| Value | Description |')
            lines.append('|-------|-------------|')
            for val in schema['enum']:
                lines.append(f'| `{val}` | |')
            lines.append('')
        elif schema.get('properties'):
            lines.append(f'**Type:** Object')
            lines.append('')
            table = render_properties_table(spec, schema)
            if table:
                lines.append(table)
                lines.append('')
        else:
            stype = schema.get('type', 'unknown')
            lines.append(f'**Type:** {stype}')
            lines.append('')

    return '\n'.join(lines)


def render_security(spec):
    """Render security schemes."""
    security_schemes = spec.get('components', {}).get('securitySchemes', {})
    if not security_schemes:
        return ''

    lines = []
    lines.append('## Security')
    lines.append('')
    lines.append('| Scheme | Type | Description |')
    lines.append('|--------|------|-------------|')
    for name, scheme in security_schemes.items():
        stype = scheme.get('type', '')
        desc = scheme.get('description', '')
        if scheme.get('scheme'):
            stype = f'{stype} ({scheme["scheme"]})'
        lines.append(f'| `{name}` | {stype} | {desc} |')
    lines.append('')
    return '\n'.join(lines)


def render_footer():
    """Render document footer."""
    lines = []
    lines.append('---')
    lines.append('')
    lines.append('*This document was auto-generated from `openapi.yaml` using the Architecture-As-Code pipeline.*')
    lines.append(f'*Generated: {datetime.now().strftime("%Y-%m-%d %H:%M:%S")}*')
    lines.append('')
    return '\n'.join(lines)


def main():
    parser = argparse.ArgumentParser(
        description='Convert OpenAPI YAML to GitLab-flavored Markdown with diagrams'
    )
    parser.add_argument('input', help='Path to OpenAPI YAML file')
    parser.add_argument('-o', '--output', help='Output Markdown file path',
                        default=None)
    parser.add_argument('--no-diagrams', action='store_true',
                        help='Skip diagram generation')
    parser.add_argument('--no-sequence', action='store_true',
                        help='Skip sequence diagrams for endpoints')

    args = parser.parse_args()

    # Default output filename
    if not args.output:
        base = os.path.splitext(os.path.basename(args.input))[0]
        args.output = f'{base}-api-docs.md'

    print(f'📖 Loading OpenAPI spec: {args.input}')
    spec = load_spec(args.input)

    print(f'📝 Generating Markdown documentation...')

    sections = []
    sections.append(render_info(spec))
    sections.append(render_toc(spec))
    sections.append(render_servers(spec))

    if not args.no_diagrams:
        sections.append(render_architecture_diagram(spec))

    sections.append(render_endpoints_by_tag(spec))
    sections.append(render_schemas(spec))
    sections.append(render_security(spec))
    sections.append(render_footer())

    output = '\n'.join(sections)

    with open(args.output, 'w') as f:
        f.write(output)

    # Stats
    paths = spec.get('paths', {})
    endpoint_count = sum(
        len([m for m in methods if m in ('get', 'post', 'put', 'delete', 'patch')])
        for methods in paths.values()
    )
    schema_count = len(spec.get('components', {}).get('schemas', {}))

    print(f'✅ Generated: {args.output}')
    print(f'   📊 Endpoints: {endpoint_count}')
    print(f'   📦 Schemas:   {schema_count}')
    print(f'   📄 Size:      {len(output):,} characters')


if __name__ == '__main__':
    main()
