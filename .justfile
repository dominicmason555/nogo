# Lists the options
@default:
    just --list

# Runs the site generator
run:
    clj -M src/nogo.clj
