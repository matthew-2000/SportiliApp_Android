> Aggiornamento del 24 settembre 2026: i punti residui 5, 6 e 11 sono descritti
> in [REMAINING-FINDINGS.md](REMAINING-FINDINGS.md), insieme al limite backend del punto 2.

# Aggiornamento: editor secondari, chiavi storico e SDK iOS — 25 settembre 2026

- Gli editor admin di giorno, gruppo ed esercizio usano transazioni con snapshot
  iniziale: conservano modifiche remote indipendenti e campi sconosciuti, mentre
  rifiutano i conflitti senza sovrascrivere. Le creazioni sono atomiche.
- iOS e Android producono chiavi `exerciseData` stabili e indipendenti dalla
  lingua; le chiavi legacy presenti continuano a essere usate senza migrazione.
- Un host temporaneo compila il client iOS reale e prova Firebase Auth/RTDB sugli
  emulatori locali: login valido, assente/non valido, errore e retry, refresh,
  cambio utente, realtime e note con storico/campi sconosciuti preservati.

Esiti: 30 test unitari Android, 11 test Android con SDK/RTDB locale, 17 test di
compatibilità Firebase, i tre runner Swift e l'host SDK iOS superati. Nessuna
regola, configurazione Auth o dato di produzione modificato. Il codice admin del
punto 2 resta intenzionalmente invariato su richiesta.

---

# Aggiornamento: login, concorrenza, osservatori e giorni residui — 23 settembre 2026

I quattro interventi elencati nella sezione storica sotto sono implementati.
Accessi tramite codice, schema, regole, Auth, Storage e istanti di scadenza
restano compatibili. Nessun deploy Firebase o pubblicazione negli store.

## Modifiche

- **iOS:** lettura del solo `/users/{code}` dopo il controllo admin `/fausto`;
  errori, cancellazione, timeout di 20 secondi, nuovo tentativo e protezione da
  risposte tardive. I caratteri non validi non raggiungono un percorso utente;
  il confronto del codice admin continua a precedere questa validazione.
- **iOS:** un osservatore scheda per manager, rimozione al refresh/cambio utente,
  cancellazione e deinit; callback vecchie escluse anche dopo il dispatch sulla UI.
- **Android admin:** creazione utente con transazione; salvataggio scheda tramite
  confronto tra snapshot iniziale, modifiche locali e stato corrente, all’interno
  di una transazione. Lo snapshot locale è escluso dalla serializzazione Firebase.
  Solo i campi modificati vengono applicati; dati correnti e campi sconosciuti
  restano intatti. Eliminazioni intenzionali consentite, salvo modifiche remote
  sul contenuto eliminato. Il riordino conserva i dati sconosciuti insieme al
  giorno e rifiuta il salvataggio se nel frattempo è cambiato il sottoalbero giorni.
  I conflitti mostrano un messaggio che invita a riaprire la scheda. Una scheda
  eliminata da un altro client non viene ricreata dal salvataggio.
- **Entrambi:** giorni interi di calendario sotto una settimana, singolare/plurale
  e meno di un giorno. Nessuna modifica alle condizioni della richiesta.
  Il salvataggio Android conserva anche l’orario iniziale se la data non è editata.

## Verifiche automatiche

- Android: `testDebugUnitTest`, **27 test superati**; build Debug e Release riuscite.
- Android UI/strumentali ordinari: **9 test superati**, incluso errore di conflitto
  visibile; 5 test RTDB saltati in questa esecuzione perché eseguiti separatamente.
- SDK Android + RTDB locale: **5 test superati**, inclusi due client indipendenti
  che creano lo stesso codice e due editor concorrenti (un solo vincitore).
  Verificati conflitti, richieste, note, storico, campi sconosciuti, interi a 64 bit,
  eliminazione intenzionale, mancata ricreazione e rifiuto di scrittura.
- Emulatori RTDB/Storage: **17 test superati** sui contratti legacy esistenti.
- iOS: **6 gruppi** sulle scadenze; runner su codice reale con doppi SDK per login
  e osservatori, compresi timeout, retry, cambio utente e deinit, tutti superati.
- iOS: build Debug Simulator e Release dispositivo, `CODE_SIGNING_ALLOWED=NO`.

Comandi riproducibili: quelli sotto per build e Android; in iOS eseguire
`python3 Tests/run_model_tests.py` e `python3 Tests/run_lifecycle_tests.py`.
Le prove RTDB scrivono soltanto sul progetto `demo-sportili-compat` locale.

## Verifiche visive e limiti

Tre anteprime locali iOS controllate su iPhone 17 Pro / iOS 26.2: giorni residui
non rossi, scadenza con pulsante richiesta, richiesta già inviata senza pulsante.
Screenshot e log di questa esecuzione in `/tmp/sportili-home-final`.
Le etichette Android sono verificate automaticamente, non con ispezione visiva.

Il runner iOS simula il confine Firebase: non prova rete/Auth con il vero SDK né
un login completo su dispositivo. Le build sono senza distribuzione e non sono
stati installati i binari degli store. Le vecchie versioni possono ancora usare
scritture integrali: queste correzioni non impediscono successive sovrascritture
provenienti da client legacy. Le regole legacy restano esposte come documentato.
Un booleano senza revisione non permette di distinguere una richiesta cancellata
e poi reinviata con lo stesso valore durante l’editing; non si aggiunge uno schema
per aggirare questo limite. Gli altri editor specifici di giorno/gruppo/esercizio
non sono stati riprogettati da questo intervento sul salvataggio della scheda.

Prossimo passo: prima di distribuire, assegnare versioni/build appropriate e
provare login/retry e cambio utente con il vero SDK iOS in ambiente locale.

---

# Verifica delle cinque correzioni — 23 settembre 2026

Ambito: conservazione dei dati nel cambio nome admin, ordinamento senza
riscrittura automatica della scheda, conservazione/completamento esplicito della
richiesta di cambio, errori negli avvisi Android e scadenza effettiva su iOS.
Nessuna nuova funzionalità di accesso, migrazione o modifica Firebase pubblicata.

## Risultati

| Controllo | Esito |
| --- | --- |
| Android `testDebugUnitTest` | 17 test, nessun errore |
| Android UI, Pixel 8 emulator Android 14/API 34 | 8 test superati: 6 admin, 1 avvisi con errore/riprova, 1 controllo del contesto app |
| Android repository + RTDB Emulator | 2 test superati con il vero SDK: conservazione dati e gestione scrittura negata |
| Firebase RTDB/Storage emulati | 17 test superati: 12 compatibilità e 5 dimostrazioni dei limiti di sicurezza |
| iOS modelli Foundation | 5 gruppi di regressioni sulla scadenza superati, incluso cambio dell'ora |
| iOS HomeView su iPhone 17 Pro, iOS 26.2 | 3 scenari controllati visivamente: ultima settimana, scaduta, richiesta inviata |
| Build Android | Debug e Release completate |
| Lint Android Debug | Nessun errore; 61 warning e 2 hint |
| Build iOS | Debug per Simulator e Release per dispositivo completate con firma disabilitata |

Le build presentano avvisi su API deprecate e altre segnalazioni non bloccanti;
non sono stati eseguiti aggiornamenti generici delle dipendenze o refactoring.
Android usa l'incremento versione già presente nel workspace: 1.3.5 (31).
La build iOS resta 1.5.3 (15); prima di una nuova distribuzione occorre assegnare
un numero di build non già usato.

## Riproduzione

Android, dalla root della repo, con Java 21:

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
./gradlew :app:connectedDebugAndroidTest --console=plain
./gradlew :app:assembleRelease --console=plain
```

La suite UI ordinaria salta le due prove RTDB native se non sono abilitate;
queste si eseguono separatamente con `npm run test:android-repository` da questa
cartella. `npm test` esegue i 17 test delle regole. Prerequisiti e isolamento
sono descritti in [README.md](README.md).

Nella repository iOS, `Tests/README.md` documenta il runner dei modelli e quello
delle schermate. Le build complete sono state eseguite con lo schema
`SportiliApp`, configurazione Debug su iOS Simulator e Release con destinazione
`generic/platform=iOS`, entrambe con `CODE_SIGNING_ALLOWED=NO`.

## Limiti delle prove

- Nessuna lettura/scrittura di dati reali è stata usata per i test. Nessuna
  modifica a regole, account Auth o provider in produzione.
- La UI Android è stata provata con dati in memoria; il repository Android è
  stato verificato separatamente contro RTDB locale. Non è un unico test
  completo dal login al salvataggio sul backend reale.
- iOS usa HomeView e modelli reali in un host locale separato, senza configurare
  Firebase. La verifica visiva non prova login né invio della richiesta.
- I contratti delle vecchie versioni sono coperti dai test delle regole; non
  sono stati installati e provati i binari distribuiti agli utenti.
- Le build non sono pacchetti firmati per gli store. Commit e push non
  distribuiscono automaticamente questi aggiornamenti agli utenti.
- I test verdi documentano le correzioni e la compatibilità degli scenari
  coperti; non risolvono l'esposizione delle regole legacy.

## Interventi allora rimasti (completati nell’aggiornamento sopra)

1. **Login iOS:** `SportiliApp/LoginView.swift` legge tutto `/users` e non gestisce
   la cancellazione delle letture di `/users` e `/fausto`. Leggere soltanto il
   profilo del codice inserito e terminare il caricamento con un messaggio e
   possibilità di riprovare in caso di errore, conservando lo stesso login.
2. **Scritture admin concorrenti:** in `newadmin/data/FirebaseRepositoryImpl.kt`,
   `addUser` separa controllo di esistenza e creazione, e `updateWorkoutCard`
   sostituisce tutta la scheda. Rendere atomica la creazione e valutare scritture
   mirate per non sovrascrivere modifiche o richieste arrivate durante l'editing;
   aggiungere casi di concorrenza sull'emulatore prima della correzione.
3. **Osservatori iOS:** `SportiliApp/Model/Scheda.swift` apre un nuovo
   `observe(.value)` a ogni caricamento senza conservarne/rimuoverne l'handle;
   `SchedaViewModel.fetchScheda` è richiamato anche dai refresh. Gestire un solo
   osservatore per scheda e rimuoverlo quando cambia utente o termina l'uso.
4. **Ultima settimana:** la schermata iOS corretta mostra ancora “0 settimane
   rimanenti” quando restano alcuni giorni. Mostrare i giorni residui renderebbe
   più chiaro il messaggio senza cambiare regole di scadenza o dati Firebase.
