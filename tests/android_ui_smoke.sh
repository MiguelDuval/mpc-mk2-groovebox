dump_device_ui_retry() {
  local requested_remote_path="$1" host_path="$2"
  local attempt_timeout="${3:-8}" max_attempts="${4:-4}" attempt
  local scratch_path="${host_path%.xml}-retry-current.xml"
  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    rm -f "$scratch_path"
    # dump_device_ui removes its output before each attempt. Write to a scratch
    # path so a timeout cannot erase the last validated host hierarchy needed
    # for diagnosis. Publish only a complete, parseable new snapshot.
    if dump_device_ui "$requested_remote_path" "$scratch_path" "$attempt_timeout"; then
      if ! mv -f -- "$scratch_path" "$host_path"; then
        echo "ERROR: could not publish fresh UI hierarchy: $host_path"
        return 1
      fi
      return 0
    fi
    rm -f "$scratch_path"
    if (( attempt < max_attempts )); then
      echo "Retrying fresh UI hierarchy capture ($((attempt + 1))/$max_attempts): $requested_remote_path"
      sleep 1
    fi
  done
  echo "ERROR: fresh UI hierarchy did not stabilize after $max_attempts bounded attempts: $requested_remote_path"
  return 1
}