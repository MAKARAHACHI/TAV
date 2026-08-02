---
name: followup-infra
description: Connect to and operate the FollowUp WhatsApp/OTP VM, which now runs IPv6-only (no external IPv4). Use when SSHing to whatsapp-bot-vm, debugging the tunnel / Baileys / OTP, checking why a message won't send, or touching the IPv6 networking. Run /followup-infra.
---

# FollowUp — Infra & IPv6-only VM Runbook

The backend (baileys + OTP + redis + cloudflared) runs on **`whatsapp-bot-vm`**, GCP project
**`tav-app-2026`**, zone **`us-central1-a`**, e2-micro. As of 2026-08-02 it has **NO external
IPv4** — external IPv6 only — to keep cost ~₪0. This changes how you connect and how egress works.
Memory: [[followup-vm-ipv6-only]]. For releases/OTP-user ops see skill `followup-release`.

## Golden rules
- **SSH ALWAYS needs `--tunnel-through-iap`** (no public IPv4 to SSH to). Plain `gcloud compute ssh` will hang/fail.
- Each `gcloud ssh` is a fresh session; docker needs `sudo`.
- **Nested quotes break the outer bash.** For any non-trivial remote command, WRITE A SCRIPT and `gcloud compute scp` it, then `bash ~/script.sh`. Don't inline `node -e "..."` with quotes over SSH.
- **Never print/commit secrets**: cloudflared tunnel token (in the container args + a compose/env on the box), BAILEYS_API_KEY, ADMIN_KEY. Mask with `sed -E 's/eyJ[A-Za-z0-9_-]+/MASKED/g'`.
- Don't restart baileys/redis just to fix cloudflared. Don't refresh the Cloudflare tunnel token.

## Connect
```
gcloud compute ssh whatsapp-bot-vm --zone=us-central1-a --tunnel-through-iap --command='<cmd>'
```
Copy a script up:
```
gcloud compute scp <local.sh> whatsapp-bot-vm:/home/danie/<name>.sh --zone=us-central1-a --tunnel-through-iap
```
IAP prerequisites (already set): API `iap.googleapis.com` enabled; firewall `allow-iap-ssh` = ingress tcp:22 from `35.235.240.0/20`.

## The IPv6 architecture (all 6 layers must stay IPv6 or sending breaks)
1. VPC `default` = **custom mode** (was auto; one-way switch).
2. Subnet `default`/us-central1 = `IPV4_IPV6`, ipv6-access-type `EXTERNAL`.
3. VM NIC = dual-stack, **external IPv6 only**, no external IPv4. Internal IPv4 `10.128.0.3` kept.
4. **cloudflared** = `docker run ... --network host cloudflare/cloudflared tunnel --no-autoupdate --edge-ip-version 6 run --token <TOKEN>`. The `--edge-ip-version 6` is mandatory.
5. **Docker daemon** `/etc/docker/daemon.json` = `{"experimental":true,"ip6tables":true,"ipv6":true,"fixed-cidr-v6":"fd00:dead:beef::/48"}` (NAT66 for container egress).
6. **compose** `~/baileys-api/docker-compose.yml` has `networks: default: {enable_ipv6: true, ipam: {config: [{subnet: fd00:cafe:1::/64}]}}`. Subnet must NOT overlap the daemon fixed-cidr-v6.

## Health check (fastest first)
1. External OTP (the real thing the app hits):
   ```
   curl -s https://otpf.ta-v.com/health            # -> {"ok":true}
   curl -s -X POST https://otpf.ta-v.com/otp/request -H 'content-type: application/json' -d '{"phone":"0542266890"}'
   # {"sent":true} = Baileys sent over IPv6. sent:true is only returned on a real 2xx from baileys (§2 honest).
   ```
2. Containers: `... --command='sudo docker ps --format "{{.Names}} | {{.Status}}"'` — expect app+redis healthy, otp + cloudflared up.
3. cloudflared reached edge over IPv6: `... 'sudo docker logs cloudflared-tunnel --tail 20' | grep "Registered tunnel connection"` — the `ip=` must be `2606:4700:...` (Cloudflare IPv6).

## Debugging "can't send / no code arrives"
Usually a broken IPv6 egress layer. Checks (put in a script, scp, run):
- Container has a GLOBAL IPv6? `sudo docker exec baileys-api-app-1 sh -c 'ip -6 addr | grep inet6 | grep -v ::1'` (expect `fd00:cafe:1::x scope global`; `NO_GLOBAL_IPV6` = layer 5/6 broke).
- Container reaches WhatsApp over IPv6? (no curl in image, use node):
  `sudo docker exec baileys-api-app-1 node -e 'require("https").get("https://web.whatsapp.com",{timeout:9000},r=>{console.log(r.statusCode);process.exit(0)}).on("error",e=>{console.log(e.code);process.exit(1)})'` → expect `200`.
- Network has ipv6? `sudo docker network inspect baileys-api_default --format 'ipv6={{.EnableIPv6}}'` → true.
- Baileys log (mask token): `sudo docker logs baileys-api-app-1 --since 3m 2>&1 | grep -iE 'connection|open|close|econn|logout' | grep -v webhook`.
- Session persists in the `baileys-api_redis-data` volume; `docker compose down/up` is safe and does NOT log you out. cloudflared is standalone (not in compose) — compose down won't touch it.

## Facts that bite
- **GitHub is IPv4-only** (no AAAA). Don't `curl`/download from github.com on this box at runtime. Use Cloudflare apt repo for cloudflared, NodeSource for Node, npm/DockerHub (all have IPv6).
- Baileys needs **Node 20+**.
- WhatsApp/Meta hosts have IPv6 (`2a03:2880:...`).

## Rollback (only if IPv6 path ever fails)
Restore external IPv4 (VPC-custom + dual-stack subnet stay, harmless):
```
gcloud compute instances add-access-config whatsapp-bot-vm --zone=us-central1-a --access-config-name=external-nat
```
Then revert cloudflared to no `--edge-ip-version` if desired. This brings back the ~₪10/mo charge.

## Cost state
external IPv6 = free; e2-micro + 30GB pd-standard = free-tier; Cloudflare Tunnel = free;
Network Intelligence (`networkmanagement.googleapis.com`) = disabled. Net ~₪0/mo.
