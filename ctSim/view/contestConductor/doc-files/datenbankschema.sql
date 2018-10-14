-- phpMyAdmin SQL Dump
-- version 2.8.1
-- http://www.phpmyadmin.net
--
-- Host: 10.10.22.242
-- Erstellungszeit: 09. Oktober 2006 um 20:23
-- Server Version: 5.0.22
-- PHP-Version: 5.1.4
--
-- Datenbank: `ctbot-contest`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_blacklist`
--

DROP TABLE IF EXISTS `ctsim_blacklist`;
CREATE TABLE `ctsim_blacklist` (
  `id` int(11) NOT NULL auto_increment,
  `suchwort` varchar(255) collate latin1_german1_ci NOT NULL default '',
  `erklaerung` text collate latin1_german1_ci,
  `updatedatum` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Betrifft nur Build-Skript: verbotene Schluesselwoerter' AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_bot`
--

DROP TABLE IF EXISTS `ctsim_bot`;
CREATE TABLE `ctsim_bot` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) collate latin1_german1_ci NOT NULL,
  `team` int(11) NOT NULL,
  `codestatus` varchar(20) collate latin1_german1_ci NOT NULL default 'leer',
  `quelltext` mediumblob COMMENT 'Vom Leser hochgeladener Quelltext. Warum Blob statt Text? -- Keine Ahnung',
  `patchoutput` text collate latin1_german1_ci,
  `bin` mediumblob COMMENT 'Vom Build-Skript hochgeladener ausfuehrbarer Code (ELF)',
  `compileroutput` text collate latin1_german1_ci COMMENT 'Vom Build-Skript geschrieben: Compilerfehler/-Warnungen vom Kompilieren des Quelltexts',
  `updatedatum` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `ctsim_bot_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Enthaelt die Bots' AUTO_INCREMENT=50 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_game`
--

DROP TABLE IF EXISTS `ctsim_game`;
CREATE TABLE `ctsim_game` (
  `id` int(11) NOT NULL auto_increment,
  `level` int(11) NOT NULL COMMENT 'Nummer des Levels (8 fuer Achtelfinale, 4 fuer Viertelf. usw. Sonderwerte: 0 = Spiel um den 3. Platz, -1 = Vorrunde). Im Java-Code Schluessel zusammen mit ''game''',
  `game` int(11) NOT NULL COMMENT 'Laufende Nummer des Spiels innerhalb des Levels. Im Java-Code Schluessel zusammen mit ''level''; faengt mit 1 an (nicht 0)',
  `bot1` int(11) default NULL COMMENT 'Der eine Spieler (Fremdschluessel ctsim_bot)',
  `bot2` int(11) default NULL COMMENT 'Der andere Spieler (Fremdschluessel ctsim_bot)',
  `scheduled` datetime default NULL COMMENT 'Geplante Startzeit',
  `finishtime` int(11) default NULL COMMENT 'Simzeit [ms], wann das Ziel ueberschritten wurde',
  `bot1restweg` double(20,10) default NULL COMMENT 'Strecke [m], wie weit Bot 1 am Spielende noch vom Zielfeld entfernt war. 0, falls er im Ziel steht',
  `bot2restweg` double(20,10) default NULL COMMENT 'Strecke [m], wie weit Bot 2 am Spielende noch vom Zielfeld entfernt war. 0, falls er im Ziel steht',
  `state` varchar(20) collate latin1_german1_ci default 'not init' COMMENT 'Moegliche Werte: "not init", "wait for bot2", "ready to run", "running", "game over"',
  `winner` int(11) default NULL COMMENT 'Gewinner (Fremdschluessel ctsim_bot)',
  `screenshot` mediumblob COMMENT 'Zielfoto von dem Moment, wo die Ziellinie ueberschritten wurde. Zeigt Parcours und Bots (wie von ctSim dargestellt). Format PNG in undefinierter Groesse, nur fuer internen Gebrauch',
  `updatedatum` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Enthaelt die einzelnen Spiele -- siehe auch turnierbaum.pdf' AUTO_INCREMENT=575 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_level`
--

DROP TABLE IF EXISTS `ctsim_level`;
CREATE TABLE `ctsim_level` (
  `id` int(11) NOT NULL,
  `parcours` text collate latin1_german1_ci COMMENT 'Spezifikation eines Parcours als XML-String (nicht Dateiname oder so). Manuell zu befuellen.',
  `scheduled` datetime NOT NULL default '2007-01-01 00:00:00' COMMENT 'Startzeit des ersten Spiels dieses Levels. Manuell zu vergeben.',
  `gametime` int(11) NOT NULL default '600000' COMMENT 'Hoechstlaenge [Simzeit, ms] eines Spiels auf diesem Level. Bei Ueberschreitung wird das Spiel abgebrochen',
  `gametime_real` int(11) NOT NULL default '600' COMMENT 'Laenge [Realzeit, s] eines Spiels auf diesem Level. Nur wichtig fuer Planung -- Spiele werden so angelegt, dass ihre Startzeitpunkte um diese Zeitspanne auseinanderliegen',
  `updatedatum` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  `screenshot` mediumblob COMMENT 'Screenshot des Parcours ohne Bots, PNG, 1 Block im Parcours = 38x38 px, Parcours sind verschieden gross, aber maximal 25x18 Bloecke',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Karten und so fuer Achtel-, Viertel-, Halbfinale usw.';

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_log`
--

DROP TABLE IF EXISTS `ctsim_log`;
CREATE TABLE `ctsim_log` (
  `id` int(11) NOT NULL auto_increment,
  `game` int(11) NOT NULL COMMENT 'Fremdschluessel ctsim_game / Vorsicht, bezieht sich auf ctsim_game.id, NICHT auf ctsim_game.game',
  `logtime` int(11) default NULL COMMENT 'Simzeit [ms], auf die sich der Logeintrag bezieht',
  `pos1x` float default NULL COMMENT 'Zentrum von Bot 1: X-Koordinate [m] von der linken unteren Parcoursecke',
  `pos1y` float default NULL COMMENT 'Zentrum von Bot 1: Y-Koordinate [m] von der linken unteren Parcoursecke',
  `head1x` float default NULL COMMENT 'Bot 1: X-Komponente Vektor der Blickrichtung',
  `head1y` float default NULL COMMENT 'Bot 1: Y-Komponente Vektor der Blickrichtung',
  `state1` int(11) default NULL COMMENT 'Siehe Extradatei "dbfeld-ctsim-log-state.txt"',
  `pos2x` float default NULL COMMENT 'Zentrum von Bot 2: X-Koordinate [m] von der linken unteren Parcoursecke',
  `pos2y` float default NULL COMMENT 'Zentrum von Bot 2: Y-Koordinate [m] von der linken unteren Parcoursecke',
  `head2x` float default NULL COMMENT 'Bot 2: X-Komponente Vektor der Blickrichtung',
  `head2y` float default NULL COMMENT 'Bot 2: Y-Komponente Vektor der Blickrichtung',
  `state2` int(11) default NULL COMMENT 'Siehe Extradatei "dbfeld-ctsim-log-state.txt"',
  PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Zeigt, welcher Bot wo ist (waehrend Spiel Insert alle 10 ms)' AUTO_INCREMENT=170607 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur fuer Tabelle `ctsim_team`
--

DROP TABLE IF EXISTS `ctsim_team`;
CREATE TABLE `ctsim_team` (
  `id` int(11) NOT NULL auto_increment,
  `name` varchar(255) collate latin1_german1_ci NOT NULL,
  `passwort` varchar(32) collate latin1_german1_ci NOT NULL,
  `leiter_email` varchar(255) collate latin1_german1_ci NOT NULL,
  `leiter_name` varchar(255) collate latin1_german1_ci NOT NULL,
  `leiter_adresse` varchar(255) collate latin1_german1_ci NOT NULL,
  `leiter_plz` varchar(255) collate latin1_german1_ci NOT NULL,
  `leiter_ort` varchar(255) collate latin1_german1_ci NOT NULL,
  `leiter_land` varchar(255) collate latin1_german1_ci NOT NULL,
  `teammitglieder` text collate latin1_german1_ci,
  `selbstdarstellung` text collate latin1_german1_ci,
  `kommentar` text collate latin1_german1_ci,
  `verifiziert` int(11) NOT NULL default '0',
  `anmeldedatum` datetime default NULL,
  `updatedatum` timestamp NOT NULL default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`),
  UNIQUE KEY `ctsim_team_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci COMMENT='Name, Adresse und so von den Wettbewerbsteilnehmern' AUTO_INCREMENT=1 ;
