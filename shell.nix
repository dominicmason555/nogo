{ pkgs ? import (fetchTarball "https://github.com/NixOS/nixpkgs/tarball/nixos-23.11") {} }:

pkgs.mkShell {
    name = "nogo-shell";
    LOCALE_ARCHIVE = "${pkgs.glibcLocales}/lib/locale/locale-archive";
    buildInputs = [ (import ./default.nix { inherit pkgs; }) ];
    shellHook = ''
        echo ""
        echo ">>> Entering Nix Shell for Nogo"
        echo ""
        echo "Run commands using just <command>"
        just
    '';
}
