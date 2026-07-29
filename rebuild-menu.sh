#!/bin/bash
set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"

declare -a SCRIPTS=(
  "all|Rebuild ALL services (rebuild.sh)"
  "rebuild-api-gateway|API Gateway"
  "rebuild-calculator-service|Calculator Service"
  "rebuild-client-service|Client Service"
  "rebuild-discovery-server|Discovery Server"
  "rebuild-employee-service|Employee Service"
  "rebuild-file-service|File Service"
  "rebuild-frontend|Frontend"
  "rebuild-generate-data-service|Generate Data Service"
  "rebuild-material-service|Material Service"
  "rebuild-order-service|Order Service"
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

    if [ "$script_name" = "all" ]; then
      echo ""
      echo "=============================="
      echo " Running: $label"
      echo "=============================="
      echo ""
      exec "${SCRIPTS_DIR}/rebuild.sh"
    fi

    script_path="${SCRIPTS_DIR}/${script_name}.sh"

    if [ ! -f "$script_path" ]; then
      echo "Script not found: $script_path"
      exit 1
    fi

    echo ""
    echo "=============================="
    echo " Running: $label"
    echo "=============================="
    echo ""
    exec "$script_path"
  else
    echo "Invalid choice: $choice"
    exit 1
  fi
}

show_menu
