# Logging helpers
success() { echo -e "$(tput setaf 2)✔ $*$(tput sgr0)"; }
error()   { echo -e "$(tput setaf 1)✖ $*$(tput sgr0)"; }
info()    { echo -e "$(tput setaf 4)ℹ $*$(tput sgr0)"; }
warn()    { echo -e "$(tput setaf 3)! $*$(tput sgr0)"; }
