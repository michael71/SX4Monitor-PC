# SX4Monitor-PC
Selectrix (Model Railroad) Monitor and Control SW (PC, JAVA) via SXnet network, see <a href="https://opensx.net">opensx.net/sx4</a> .

## Das System:
Selectrix™ ist ein relativ einfaches Digitalsystem (in der ursprünglichen Version) – es gibt 112 Adressen (auch „Kanäle“ genannt) mit jeweils 8 Bit Daten, die regelmäßig wiederholt werden. Daher kann man den Gesamtzustand des Systems jeweils gut auf einem Bildschirm darstellen, es gibt bereits einige sogenannte „SX Monitor“ Programme. Jede diese 112 Adressen kann entweder zur Loksteuerung, Weichen- oder Signalsteuerung oder für Rückmeldezwecke (Belegtmelder/Sensoren) verwendet werden - es gibt also keinen separaten Rückmeldebus. DAS IST ALLES! Einfach und zweckmäßig.

## Das SX4Monitor-PC Programm: 
Das Programm entstand aus dem Wunsch heraus, einen Selectrix-"Monitor" auch unter Linux verwenden zu können. Wie bei allen ähnlichen Programmen gibt es die Gesamtübersicht, den „SX Monitor“ - mit der Besonderheit, dass nur Kanäle angezeigt werden, die von „0“ verschiedene Daten haben. Zur Verbindung mit dem Interface der Anlagenzentrale (per seriellem Interface) muss vor dem Start des Monitors noch der "SX4" Daemon gestartet werden, der die seriellen Kommandos auf das SXnet (TCP/IP auf Port 4104) übermitttelt, siehe <a href="https://github.com/michael71/SX4">github.com/michael71/SX4</a>.

Zur Anzeige im Monitor wird die übliche SX-Darstellung gewählt mit Bit 1 bis Bit 8, wobei das niederwertige zuerst gezeigt wird.



## Licence
GPL v3.0

