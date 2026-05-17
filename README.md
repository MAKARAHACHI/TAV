# FollowUp Nadlan Context System

This package contains the operational project context system for the Android app **FollowUp Nadlan**.

Use it as the first repo setup task before asking Codex or Claude Code to build application code.

## Install into a repo

Copy these folders into the project root:

```txt
context/
.agents/skills/
```

Then start every coding chat with:

```txt
Read context/NEW_CHAT_BOOTSTRAP.md first.
Load .agents/skills/followup-nadlan-vision-guardian/SKILL.md.
Do not implement code before checking the roadmap and guardrails.
```

## Product summary

FollowUp Nadlan is an external-distribution Android app for Israeli real-estate agents. It detects that a phone call ended, identifies the last call number when allowed, shows a follow-up card, helps the agent compose a real-estate WhatsApp message from templates/blocks, allows snoozing the follow-up, and opens WhatsApp via wa.me so the user manually presses Send.

The product is not a CRM, not a WhatsApp automation bot, not an Accessibility-based sender, and not a Play Store-first app in v1.
