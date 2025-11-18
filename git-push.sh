#!/usr/bin/env bash
set -euo pipefail

HOST=${1:-127.0.0.1}
PORT=${2:-1082}
REMOTE=${3:-origin}
ENDPOINT=${4:-ubuntu@10.38.245.92}

if [[ -n "${PROXY_USER:-}" ]]; then
  PROXY="socks5h://${PROXY_USER}:${PROXY_PASS:-}@${HOST}:${PORT}"
else
  PROXY="socks5h://${HOST}:${PORT}"
fi

URL=$(git remote get-url "$REMOTE")

# 1) 启动 ssh -D 代理（可选）：传入 ENDPOINT 如 ubuntu@10.38.245.92 则先建立隧道
CONTROL_PATH="${HOME}/.ssh/pgrest-socks-${HOST}-${PORT}"
clean_up_flag=0
cleanup() {
  if [[ "$clean_up_flag" != 1 ]]; then
    if [[ -n "$ENDPOINT" ]]; then
      ssh -O exit -o ControlPath="$CONTROL_PATH" "$ENDPOINT" || true
    fi
    if command -v pgrep >/dev/null 2>&1; then
      pgrep -f "ssh.*-D[[:space:]]*$PORT" | xargs -r kill || true
    fi
    rm -f "$CONTROL_PATH" 2>/dev/null || true
    unset ALL_PROXY
    unset GIT_SSH_COMMAND
    clean_up_flag=1
  fi
}
trap cleanup EXIT
if [[ -n "$ENDPOINT" ]]; then
  ssh -o ControlMaster=yes \
      -o ControlPath="$CONTROL_PATH" \
      -o ControlPersist=10m \
      -D "$PORT" -N -f "$ENDPOINT"
fi

if [[ "$URL" =~ ^https?:// ]]; then
  ALL_PROXY="$PROXY" git push "$REMOTE"
else
  if command -v nc >/dev/null 2>&1; then
    GIT_SSH_COMMAND="ssh -o ProxyCommand=\"nc -x $HOST:$PORT -X 5 %h %p\"" git push "$REMOTE"
  else
    if [[ "$URL" =~ ^git@([^:]+):(.+)\.git$ ]]; then
      HTTPS_URL="https://${BASH_REMATCH[1]}/${BASH_REMATCH[2]}.git"
    elif [[ "$URL" =~ ^ssh://git@([^/]+)/(.+)\.git$ ]]; then
      HTTPS_URL="https://${BASH_REMATCH[1]}/${BASH_REMATCH[2]}.git"
    else
      echo "Unsupported SSH URL: $URL" >&2
      exit 1
    fi
    ORIG_URL="$URL"
    git remote set-url "$REMOTE" "$HTTPS_URL"
    ALL_PROXY="$PROXY" git push "$REMOTE"
    git remote set-url "$REMOTE" "$ORIG_URL"
  fi
fi

# 3) 清理 ssh 动态代理
cleanup