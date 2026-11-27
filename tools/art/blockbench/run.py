#!/usr/bin/env python3
"""Runs JavaScript inside the running Blockbench through its MCP plugin's HTTP endpoint.

    python tools/art/blockbench/run.py "1 + 1"
    python tools/art/blockbench/run.py --file some_script.js

Used to drive the drawing scripts in this folder without an MCP client attached.
"""
import json
import sys
import urllib.request

URL = "http://localhost:3000/bb-mcp"


def post(body, session=None):
    headers = {"Content-Type": "application/json", "Accept": "application/json, text/event-stream"}
    if session:
        headers["mcp-session-id"] = session
    req = urllib.request.Request(URL, data=json.dumps(body).encode(), headers=headers)
    with urllib.request.urlopen(req, timeout=1800) as resp:
        text = resp.read().decode("utf-8")
        sid = resp.headers.get("mcp-session-id")
    if text.startswith("event:") or text.startswith("data:"):
        text = "\n".join(line[5:].strip() for line in text.splitlines() if line.startswith("data:"))
    return (json.loads(text) if text.strip() else None), sid


def run(code):
    _, sid = post({"jsonrpc": "2.0", "id": 1, "method": "initialize",
                   "params": {"protocolVersion": "2025-03-26", "capabilities": {},
                              "clientInfo": {"name": "iuce-tools", "version": "1"}}})
    post({"jsonrpc": "2.0", "method": "notifications/initialized"}, sid)
    result, _ = post({"jsonrpc": "2.0", "id": 2, "method": "tools/call",
                      "params": {"name": "risky_eval", "arguments": {"code": code}}}, sid)
    if "error" in result:
        raise SystemExit(json.dumps(result["error"], ensure_ascii=False))
    out = []
    for item in result["result"].get("content", []):
        if item.get("type") == "text":
            out.append(item["text"])
    if result["result"].get("isError"):
        raise SystemExit("\n".join(out))
    return "\n".join(out)


if __name__ == "__main__":
    args = sys.argv[1:]
    if args and args[0] == "--file":
        source = open(args[1], encoding="utf-8").read()
    else:
        source = " ".join(args)
    print(run(source))
