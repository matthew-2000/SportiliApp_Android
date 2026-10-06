# S11 — QA locale

Usare un emulatore temporaneo. `S11PreviewActivity` ospita LoginContent, la shell
di produzione ContentNavigation, Giorno/dettaglio, avvisi e impostazioni. Login
AB12CD valido, RETE errore locale, altri codici invalidi. Peso e nota aggiornano
solo fixture in memoria. Immagini fallite localmente, link e logout innocui.
L'extra `screen=active` salta il login; `dark=true` usa la palette scura.
La richiesta nuova scheda non è simulata in questo host: usare S03PreviewActivity
e S03UxTest per i suoi callback controllati.

`S11AdminPreviewActivity` usa AdminNavGraph reale e un repository in memoria;
mette offline il database SDK predefinito. Verifica utenti→scheda→giorno e il
master/detail adattivo. Il catalogo remoto non viene alimentato: usare
S09PreviewActivity/S09ExerciseEditorsTest per gruppo e catalogo locali.
Entrambe le activity e i repository fake sono esclusi da Release.

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w -e class com.matthew.sportiliapp.S11NavigationTest,com.matthew.sportiliapp.S11AdminAccessibilityTest -e captureMode normal com.matthew.sportiliapp.test/androidx.test.runner.AndroidJUnitRunner
```

Ripetere con font_scale=2.0 e wm density=540 sul Pixel 2 1080 px (320 dp).
S11NavigationTest verifica tab/ritorno, errori login, aiuto, peso/nota/timer e
rotazione; S11AdminAccessibilityTest verifica spazio del form, contesto giorno
lungo, validazione con tastiera e raggiungibilità di tutte le azioni con
completamento locale della richiesta. Testata e barra azioni hanno scroll
indipendenti; il ritorno rimane sempre disponibile. Salva chiude focus/tastiera;
il test usa il nodo testo non unito per verificare l'intero errore nel viewport
dopo il resize nativo dell'IME. PNG nativi sotto
`/sdcard/Android/data/com.matthew.sportiliapp/files/S11/<captureMode>`.

S11TalkBackProbe è un osservatore di un giro guidato, da eseguire separatamente
con il servizio TalkBack realmente abilitato. Conserva il servizio con
FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES, registra gerarchie/eventi/focus per
massimo cinque minuti. Terminare con `adb shell setprop debug.sportili.s11_done true`.
Richiede almeno 12 eventi di focus accessibilità; non certifica qualità audio,
comprensibilità o intero percorso. Durante il giro usare i bounds delle sue
gerarchie per i gesti: uiautomator dump può sopprimere altri servizi.

Ripristinare font/notte/densità/servizi e fermare/rimuovere soltanto l'emulatore
QA creato. Non usare credenziali, dati o salvataggi del backend condiviso.
