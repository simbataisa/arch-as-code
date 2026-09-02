"""Task 21's Step 1 test for recompute.py, given verbatim in the task brief."""

from tst_039_recon.recompute import score_dimensions


def test_recompute_scores_false_negatives_independently():
    # Independent recomputation is why TST-039 uses Locust, not JMeter.
    seeded = {"accuracy": {"ACC-000003"}, "completeness": set(), "timeliness": set()}
    reported = {"accuracy": set(), "completeness": set(), "timeliness": set()}
    score = score_dimensions(seeded, reported)
    assert score["accuracy"]["fn"] == 1
    assert score["accuracy"]["fp"] == 0
