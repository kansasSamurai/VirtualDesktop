# Prompt template: new tool shell

Use this to have an AI assistant rough in a new VirtualDesktop tool: a menu entry that opens an
application stage with a placeholder. Fill in the four fields, then paste the whole block.

````markdown
Create the initial shell for a new VirtualDesktop tool.

- Tool title (menu text): <TITLE, e.g. Agent Auditor>
- Package: <PACKAGE, e.g. org.katacode.agents.auditor>
- Class name stem: <NAME, e.g. AgentAuditor>
- Purpose (context only, do not build it yet): <ONE OR TWO SENTENCES>

Scope: a shell only. Real content comes in a later phase.

1. In <PACKAGE>, create <NAME>Stage extending PanelApplicationStage
   (see docs/features/appstage/README.md). It adds and shows a "main" card
   holding a simple JPanel with a centered placeholder message. All
   application stage usage belongs in this package.
2. In org.jwellman.virtualdesktop.vapps, create Spec<NAME> extending
   VirtualAppSpec. It only sets the title and size (1000x700) and uses
   <NAME>Stage as its content. Always set both width and height, because
   a stage has no useful preferred size for pack().
3. Register it in virtualdesktop-java8/config/vapps-config.json so it
   appears in the menu. Do not add a desktop shortcut.
4. Icon: pick an appropriate key from
   virtualdesktop-java8/config/icon-theme.json. If none fits well, use the
   configured defaultIcon and tell me.

Constraints: Java 8, project formatting rules, no hardcoded colors,
no toolbar or services unless asked. Do not compile; I will.
Reference example: SpecAgentAuditor and AgentAuditorStage.
````

## Notes

- The reference example will gain real content over time and stop looking like a shell. The numbered
  steps stand on their own, so drop the "Reference example" line if it starts to mislead.

## Reminder: turn this into a skill

Starting a new tool is expected to be a very common task. Once this template has proved itself a
couple of times, consider adding a skill under `.cursor/skills/new-tool/` so an assistant can pick it
up from a request such as "create a new tool", with no pasting.

- Have the skill link to this file instead of copying the steps, so there is one place to update.
- Write its description around the phrases you actually use to ask for a new tool.
- Skills under `.cursor/skills/` may not be discovered by every assistant; check the ones you use.
