# Richtlinien für Erweiterungen am c't-Bot und c't-Sim (coding conventions)

Fremden Code lesen kann ganz schön anstrengend sein - ein Variablenname, der dem einen selbsterklärend vorkommt, kann auf den anderen kryptisch wirken. Wenn du eine Erweiterung schreibst und deinen Code möglichst verständlich programmierst und dokumentierst, steigen die Chancen, dass andere c't-Bot-Fans von deiner Arbeit ebenfalls profitieren. Als Hilfe haben wir ein paar Richtlinien zusammengestellt, die für Übersicht im c't-Roboter-Projekt sorgen sollen.

Zugegeben: Bislang erfüllt die offizielle Codebasis diese Richtlinie ebenfalls (noch) nicht bis ins letzte Detail - wir arbeiten aber daran das zu ändern. Deshalb kommen uns Beiträge von deiner Seite sehr entgegen, welche die aufgestellten Regeln einhalten.

Damit Pull-Requests für den Code möglichst kompatibel zueinander sind und überschaubar bleiben, sollten sie schlank sein. Faustregel: Ein Request pro Thema. Insbesondere Formatierungsänderungen usw. blähen den Request stark auf und machen ihn im Zweifelsfall inkompatibel zu anderen.

## Allgemeine Richtlinien:

* Namen von Variablen, Funktionen und alles, was direkt mit dem Quellcode zu tun hat, sollten rein englisch sein.
* Kommentare und Dokumentation können auch auf Deutsch verfasst werden.
* Variablen, die in c't-Bot und c't-Sim die gleichen Werte haben, sollten auch die gleichen Namen bekommen. Das macht vieles einfacher und vermeidet Verwechselungen.
* Alle Funktionen und Methoden sollen vollständig kommentiert werden. Hierzu gehört eine kurze Beschreibung, **was** die Funktion tut (aber nicht zwingend, **wie** sie das tut), eine Erläuterung von Übergabeparametern und Rückgabewerten sowie der unter Umständen geworfenen Exceptions. Bei trivialen Methoden wie Konstruktoren oder get()- und set()-Methoden kann die Kurzbeschreibung oft auch weggelassen werden.
* Die Kommentare zu Klassen und Methoden sollen Javadoc-konform gestaltet werden - auf diese Weise kann man aus dem Code bequem durchsuchbare HTML-Seiten generieren.
* Bei allen Änderungen bitte Einträge in die jeweilige Changelog-Datei nicht vergessen. Hier sollte neben dem Datum der Änderung, Namen und E-Mail-Adresse noch ein kurzer Text stehen, der darstellt, inwiefern die Erweiterung den Roboter oder den Simulator verbessert.
* Der komplette Code - und damit auch jede Erweiterung - steht unter der GPL. Daher müssen neu hinzugefügte Dateien auch einen GPL-Header besitzen.

### c't-Sim

* Alle Variablen und Konstanten, die sich auf Sensoren beziehen, beginnen mit 'sens' oder 'SENS', alle Aktuator-Wert mit 'act'. Auch in Methoden, die mit den Sensoren oder Aktuatoren zu tun haben, finden sich die Bezeichner 'sens' und 'akt' wieder (<code>getSensBorderPosition()</code>) .

* Bitte beachte beim Programmieren auch die üblichen Java-Coding-Conventions, wie etwa:
  * Methodennamen sollten aus Verben bestehen und mit einem kleinen Buchstaben beginnen. Bestehen Namen aus mehreren Wörtern, werden diese direkt hintereinander geschrieben, wobei alle außer dem ersten mit einem Großbuchstaben beginnen (<code>senseGroundReflectionCross()</code>) .
  * Für Variablennamen gilt die gleiche Schreibweise wie für Methoden (vorne mit einem kleinen und im Inneren mit großen ersten Buchstaben). Variablennamen sollten kurz, aber verständlich sein (<code>lightBG</code> für die BranchGroup der Lichtquellen im c't-Sim) .
  * Variablennamen aus einem einzelnen Buchstaben sind zu vermeiden, Ausnahme sind Zählervariablen, die beispielsweise nur innerhalb einer Schleife existieren (<code>for (int i = 0; i < 10; ++i) { ... }</code>).
  * Konstanten sollen in Großbuchstaben geschrieben werden, die einzelnen Wörter zusammengesetzter Bezeichner werden mit Unterstrichen getrennt (<code>LIGHT_SOURCE_REACH</code>) .

* Hast du an einer Datei etwas verändert (beispielsweise eine neue Methode hinzugefügt), kannst du dich unter dem Dokumentationstext der gesamten Klasse über das Tag "@author" mit Namen und E-Mail-Adresse verewigen.
