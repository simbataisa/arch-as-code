# Email — gửi CIO

**Người nhận:** [CIO]
**Cc:** Trưởng bộ phận Kiến trúc Giải pháp; Chủ tịch EA-Board; các lãnh đạo miền N-1; các bên liên quan EAT cấp N-2
**Chủ đề:** Bộ tài liệu rà soát G2 — Danh mục Kiến trúc Doanh nghiệp Ngân hàng (đề nghị phê duyệt song song)
**Tệp đính kèm:** deck.html (17 slide · phần trình bày ~10 phút)

---

Kính gửi anh Ngọc,

Trước cổng kiểm duyệt G2 của EA-Board, xin gửi anh bộ tài liệu rà soát cho Danh mục Kiến trúc Doanh nghiệp Ngân hàng. Chúng tôi đề nghị **phê duyệt song song** — sự chấp thuận ở cấp CIO của anh, cùng với các lãnh đạo miền N-1 và nhóm bên liên quan EAT cấp N-2 — để có thể bắt đầu biên soạn mà không phải tuần tự hóa chuỗi phê duyệt. Kế hoạch đã được thống nhất với anh Timo (Head of EAT).

## Đây là gì

Đây là một kho tri thức kiến trúc hợp nhất, có thể trích dẫn, dành cho khối Solution Architecture của Techcombank. Hiện nay, kho EAT có 22 tài liệu tham chiếu nhưng chưa có mã định danh chuẩn tắc đủ điều kiện được DAB trích dẫn, chưa có ánh xạ tuân thủ riêng cho Việt Nam, và chưa có mẫu chấp nhận NFR chính thức — điều đó có nghĩa là mỗi phiên rà soát thiết kế vẫn phụ thuộc vào tri thức ngầm và phần mô tả tuân thủ phải viết mới mỗi lần. Danh mục này được xây dựng để lấp khoảng trống đó.

## Có gì trong bộ slide

- **Bối cảnh (slide 1–3).** Hiện trạng hôm nay, cùng ba con số làm mốc cho triển khai: **141** dòng danh mục mục tiêu (ngưỡng hoàn chỉnh), **20** tài liệu bộ khởi đầu sẽ được biên soạn ở độ sâu đầy đủ tới mức runbook vận hành trong Wave 0, và **6** tài liệu xương sống chuẩn tắc mà mọi bán kính đều kế thừa.
- **Mô hình (slide 4–6).** Ba vòng đồng tâm về quy định — generic (NIST, OWASP, Well-Architected) → ngân hàng quốc tế (PCI-DSS 4.0, BCBS 239/230, SWIFT CSP, ISO 20022) → Việt Nam (SBV Circular 09/2020, Decree 13/2023 PDP, Decree 53/2022). Mô hình tài liệu xương sống và bán kính. Taxonomy.
- **20 mẫu khởi đầu (slide 7) và ngưỡng chất lượng (slide 8).** Những gì chúng ta cam kết biên soạn trong đợt này, cùng mẫu 12 phần mà mọi tài liệu phải đáp ứng — bản đồ tuân thủ ba vòng, tiêu chí chấp nhận NFR, threat model, khung runbook, ví dụ mã đa công nghệ.
- **Một ví dụ minh họa hoàn chỉnh (slide 9–10).** Phân tầng dịch vụ với các mục tiêu NFR, cùng một bản đồ tuân thủ được đi xuyên suốt từ đầu đến cuối để người rà soát thấy rõ tiêu chuẩn được áp dụng như thế nào.
- **Triển khai (slide 11–13).** Kế hoạch 7 giai đoạn với các cổng kiểm duyệt rõ ràng, đồ thị phụ thuộc (xương sống làm tuần tự → các bán kính làm song song theo 3 đợt con → tích hợp cuối), cùng timeline và effort.
- **Rủi ro, chỉ số, quyết định (slide 14–16).** Risk register kèm biện pháp giảm thiểu, các chỉ số thành công cho mức độ áp dụng / chất lượng / độ phủ, và 5 quyết định cụ thể mà chúng tôi đề nghị mỗi bên phê duyệt xác nhận.

## Chúng tôi cần gì từ mỗi bên phê duyệt

1. **Taxonomy** — cách phân tách xương sống/bán kính và mô hình tuân thủ ba vòng.
2. **Phạm vi của Wave 0** — 20 mẫu khởi đầu, còn các stub của Wave 1 sẽ được hoãn lại.
3. **Quyền sở hữu** — các lãnh đạo miền N-1 được chỉ định làm chủ tài liệu, EAT cấp N-2 được chỉ định làm bên rà soát.
4. **Trình tự** — làm phần xương sống trước theo tuần tự, sau đó triển khai các phần bán kính song song theo các đợt con.
5. **Ngưỡng chất lượng** — mẫu 12 phần và các quy tắc lint sẽ là điều kiện chặn merge.

Một quyết định thường trực sẽ được ghi nhận cho từng mục; phê duyệt một phần (ví dụ: taxonomy + ngưỡng chất lượng được duyệt, nhưng phạm vi bị hoãn) vẫn được chấp nhận và cho phép chúng ta bắt đầu biên soạn phần xương sống cho tập hạng mục đã được thông qua.

## Quy trình và thời gian

- **Phát hành bộ tài liệu:** hôm nay.
- **Cửa sổ rà soát song song:** 5 ngày làm việc. Gửi nhận xét qua issue tracker của catalog, gắn tag theo số slide.
- **Buổi walkthrough trực tiếp:** phiên 45 phút cho N-1 + EAT N-2. Tôi sẽ sắp lịch.
- **Chốt G2:** cuối tuần sau. Giai đoạn 2 (biên soạn phần xương sống) sẽ mở vào tuần kế tiếp cho các hạng mục đã được thông qua.

Lộ trình ngắn nhất qua bộ slide là **slide 3, 11, 13 và 16** — chúng ta đang bàn giao gì, bằng cách nào, khi nào, và cần anh phê duyệt những gì.

Trân trọng,
Dao Tuan Anh (Dennis)
Director, Solution Architecture · Techcombank
