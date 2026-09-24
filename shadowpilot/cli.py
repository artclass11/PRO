import json
from .engine import Action, Policy, RiskLevel, ShadowPilot

def demo():
    pilot = ShadowPilot(Policy(max_risk_to_execute=RiskLevel.LOW))
    def fake_file(args, shadow):
        return {"would_write": args["path"], "bytes": len(args["content"]), "shadow": shadow}
    def fake_payment(args, shadow):
        return {"would_charge": args["amount"], "currency": args["currency"], "shadow": shadow}
    pilot.register("write_file", fake_file)
    pilot.register("payment", fake_payment)
    actions = [
        Action("draft", "write_file", {"path": "notes.txt", "content": "hello"}, reversible=True),
        Action("charge", "payment", {"amount": 99, "currency": "USD"}, reversible=False, external_side_effect=True, estimated_cost=99),
    ]
    for action in actions:
        result = pilot.run(action, execute=False)
        print(json.dumps({"action": action.name, "mode": result.mode, "risk": result.risk.name, "impact": result.impact, "preview": result.preview}, indent=2))

def main():
    demo()

if __name__ == "__main__":
    main()
