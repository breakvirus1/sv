#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

declare -a SCRIPTS=(
  "all|Rebuild ALL services (rebuild.sh)"
  "rebuild-api-gateway|API Gateway"
  "rebuild-calculator-service|Calculator Service"
  "rebuild-client-service|Client Service"
  "rebuild-comment-service|Comment Service"
  "rebuild-discovery-server|Discovery Server"
  "rebuild-employee-service|Employee Service"
  "rebuild-file-service|File Service"
  "rebuild-frontend|Frontend"
  "rebuild-generate-data-service|Generate Data Service"
  "rebuild-material-service|Material Service"
  "rebuild-order-service|Order Service"
  "rebuild-statistic-service|Statistic Service"
)

show_menu() {
  clear
  echo "=============================="
  echo "   Rebuild Service Menu"
  echo "=============================="
  echo ""

  for i in "${!SCRIPTS[@]}"; do
    IFS='|' read -r script_name label <<< "${SCRIPTS[$i]}"
    printf " %2d) %s\n" "$((i+1))" "$label"
  done

  echo ""
  printf " %2d) %s\n" "0" "Exit"
  echo ""
  read -p "Choose service to rebuild: " choice

  if [[ "$choice" == "0" ]]; then
    echo "Exiting."
    exit 0
  fi

  if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le "${#SCRIPTS[@]}" ]; then
    index=$((choice-1))
    IFS='|' read -r script_name label <<< "${SCRIPTS[$index]}"

    script_name="${script_name#rebuild-}"

    if [ "$script_name" = "all" ]; then
      echo ""
      echo "=============================="
      echo " Running: $label"
      echo "=============================="
      echo ""
      exec "${SCRIPT_DIR}/rebuild.sh"
    fi

    echo ""
    echo "=============================="
    echo " Running: $label"
    echo "=============================="
    echo ""
    exec "${SCRIPT_DIR}/rebuild-service.sh" "$script_name"
  else
    echo "Invalid choice: $choice"
    exit 1
  fi
}

show_menu
