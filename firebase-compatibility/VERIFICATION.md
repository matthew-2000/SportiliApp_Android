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

## Prossimi interventi circoscritti

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
