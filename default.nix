{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/tarball/nixos-23.11") {} }:

pkgs.buildEnv {
    name = "nogo";
    paths = [
        pkgs.just
        pkgs.fzf
        pkgs.clojure
        pkgs.neovim
    ];
}
