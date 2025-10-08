{
  description = "Nogo static site generator";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs?ref=nixos-25.05";
  };

  outputs =
    { self, nixpkgs }:
    let
      pkgs = nixpkgs.legacyPackages.x86_64-linux;
    in
    {

      devShells.x86_64-linux.default = pkgs.mkShell {
        name = "nogo-shell";
        LOCALE_ARCHIVE = "${pkgs.glibcLocales}/lib/locale/locale-archive";
        packages = with pkgs; [
          clojure
          babashka
        ];
        shellHook = ''
          echo -e "\n>>> Entering Nix Shell for Nogo\n"
          bb tasks
          echo ""
        '';
      };

    };
}
