from shadowpilot import Action, ShadowPilot

pilot = ShadowPilot()
pilot.register("deploy", lambda args, shadow: {"would_deploy": args["service"], "shadow": shadow})

action = Action(
    name="ship-api",
    tool="deploy",
    args={"service": "payments-api"},
    reversible=False,
    external_side_effect=True,
)

result = pilot.run(action)
print(result.risk.name)
print(result.preview)
print(result.receipt["hash"])
