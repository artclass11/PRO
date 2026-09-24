from shadowpilot import Action, Policy, RiskLevel, ShadowPilot


def test_cost_signal_can_block_execution():
    calls = []
    pilot = ShadowPilot(Policy(max_risk_to_execute=RiskLevel.SAFE, max_cost=1))
    pilot.register(
        "charge",
        lambda args, shadow: calls.append(shadow) or {"preview": shadow},
    )
    result = pilot.run(
        Action("charge", "charge", {"amount": 99}, estimated_cost=99),
        execute=True,
        approved=True,
    )
    assert result.mode == "blocked"
    assert calls == [True]


def test_receipt_contains_digest():
    pilot = ShadowPilot()
    result = pilot.run(Action("noop", "noop"), execute=False)
    assert result.receipt["hash"]
    assert len(result.receipt["hash"]) == 64
