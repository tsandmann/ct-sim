# ct-sim
This is a fork of the ct-Sim code which belongs to the robotic project c't-Bot of the German c't magazine (www.heise.de/ct).
The official c't-project website can be found at http://www.heise.de/ct/projekte/c-t-Bot-und-c-t-Sim-284119.html. All related documentation is available at https://github.com/Nightwalker-87/ct-bot-doku.

The repository here contains the source code of the original c't-Sim robot simulator from 2006. Each stable release of the code is tagged and the *master* branch always points to the latest one. The (experimental) development code can be found in branch *development*. Additional branches may exist for currently developed fixes or new features, use them on your own risk and bear in mind: if it breaks, you get to keep both pieces.

Feel free to fork from this repository, add your own extensions or improvements and create a pull request to get them integrated.

## Notes
 - For a Java 7 based setup use the branch *java7*. Unfortunately the latest (unofficial) Java3D jars needed for recent Linux distributions aren't compatible with Java 7 and the official ones (1.6.2) don't work with recent mesa drivers.
 - ct-Sim does not work on recent linux distributions like Fedora, RHEL 8 / CentOS 8 or Arch Linux. OpenJDK versions newer than Java 8 (1.8.x) also have problems on Linux and macOS. It's highly recommended to use ct-Sim with Java 8.

## Continuous integration tests
| Branch              | Build status  |
|:------------------- |:------------- |
| master              | [![Build status](https://travis-ci.org/tsandmann/ct-sim.svg?branch=master "Build status of branch master")](https://travis-ci.org/tsandmann/ct-sim) |
| develop             | [![Build status](https://travis-ci.org/tsandmann/ct-sim.svg?branch=develop "Build status of branch develop")](https://travis-ci.org/tsandmann/ct-sim) |
