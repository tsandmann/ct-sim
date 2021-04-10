# ct-sim
This repository contains the source code of the c't-Bot simulator. c't-Bot and c't-Sim belong together and represent a robot project that was initiated by the magazine [c't](https://www.heise.de/ct) in 2006.

The project website can be found at [www.ct-bot.de](https://www.ct-bot.de).
All related documentation is available at [here](https://github.com/Nightwalker-87/ct-bot-doku).

The repository here contains the source code of the original c't-Sim robot simulator from 2006. Each stable release of the code is tagged and the *master* branch always points to the latest one. The (experimental) development code can be found in branch *development*. Additional branches may exist for currently developed fixes or new features, use them on your own risk and bear in mind: if it breaks, you get to keep both pieces.

Feel free to fork from this repository, add your own extensions or improvements and create a pull request to get them integrated.

## Notes
 - ct-Sim does not work on recent linux distributions like Fedora, RHEL 8 / CentOS 8 or Arch Linux. OpenJDK versions newer than Java 8 (1.8.x) also have problems on Linux and macOS. It's highly recommended to use ct-Sim with Java 8.
 - For a Java 7 based setup use the branch *java7*. Unfortunately the latest (unofficial) Java3D jars needed for recent Linux distributions aren't compatible with Java 7 and the official ones (1.6.2) don't work with recent mesa drivers.
 - To enable additional warnings move file `.settings/org.eclipse.jdt.core.prefs_warnings` to `.settings/org.eclipse.jdt.core.prefs`.

## Continuous integration tests
| Branch              | Build status  |
|:------------------- |:------------- |
| master              | [![Java CI](https://github.com/tsandmann/ct-sim/actions/workflows/ant_build.yml/badge.svg?branch=master "Build status of branch master")](https://github.com/tsandmann/ct-sim/actions/workflows/ant_build.yml) |
| develop             | [![Java CI](https://github.com/tsandmann/ct-sim/actions/workflows/ant_build.yml/badge.svg?branch=develop "Build status of branch develop")](https://github.com/tsandmann/ct-sim/actions/workflows/ant_build.yml) |

