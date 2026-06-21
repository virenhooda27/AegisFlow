# Sample Workflows

## 1. AI Code Generation Pipeline

```
Requirements Analysis → System Design → Code Generation → Test Generation → Code Review → Approval → Deploy
```

**Node Types:**
- Requirements Analysis: `AGENT` (agentType: SUMMARY)
- System Design: `AGENT` (agentType: SUMMARY)  
- Code Generation: `AGENT` (agentType: SUMMARY)
- Test Generation: `AGENT` (agentType: SUMMARY)
- Code Review: `AGENT` (agentType: RECOVERY)
- Approval: `APPROVAL`
- Deploy: `SHELL`

---

## 2. Incident Recovery Pipeline

```
Alert Ingestion → Log Analysis → Root Cause Analysis → Recovery Plan → Approval → Execute Fix → Verify
```

**Node Types:**
- Alert Ingestion: `HTTP`
- Log Analysis: `SQL`
- Root Cause Analysis: `AGENT` (agentType: RECOVERY)
- Recovery Plan: `AGENT` (agentType: RECOVERY)
- Approval: `APPROVAL`
- Execute Fix: `SHELL`
- Verify: `HTTP`

---

## 3. Data Processing Pipeline

```
Ingest → Validate → Transform → Aggregate → Publish
```

**Node Types:**
- Ingest: `HTTP`
- Validate: `SQL`
- Transform: `JAVA`
- Aggregate: `SQL`
- Publish: `HTTP`

---

## How to Create

Use the Workflow Editor UI or POST to `/api/workflows`:

```json
{
  "name": "AI Code Generation Pipeline",
  "description": "End-to-end AI-assisted code generation with human approval",
  "nodes": [
    { "nodeKey": "requirements", "name": "Requirements Analysis", "type": "AGENT", "config": { "agentType": "SUMMARY", "prompt": "Analyze requirements" } },
    { "nodeKey": "design", "name": "System Design", "type": "AGENT", "config": { "agentType": "SUMMARY", "prompt": "Design the system" } },
    { "nodeKey": "codegen", "name": "Code Generation", "type": "AGENT", "config": { "agentType": "SUMMARY", "prompt": "Generate code" } },
    { "nodeKey": "testgen", "name": "Test Generation", "type": "AGENT", "config": { "agentType": "SUMMARY", "prompt": "Generate tests" } },
    { "nodeKey": "review", "name": "Code Review", "type": "AGENT", "config": { "agentType": "RECOVERY", "prompt": "Review code for issues" } },
    { "nodeKey": "approval", "name": "Human Approval", "type": "APPROVAL", "config": { "title": "Approve code for deployment" } },
    { "nodeKey": "deploy", "name": "Deploy", "type": "SHELL", "config": { "command": "echo 'Deploying...'" } }
  ],
  "edges": [
    { "sourceKey": "requirements", "targetKey": "design" },
    { "sourceKey": "design", "targetKey": "codegen" },
    { "sourceKey": "codegen", "targetKey": "testgen" },
    { "sourceKey": "testgen", "targetKey": "review" },
    { "sourceKey": "review", "targetKey": "approval" },
    { "sourceKey": "approval", "targetKey": "deploy" }
  ]
}
```
