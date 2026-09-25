# Chiusura dei problemi residui — aggiornata il 25 settembre 2026

## Stato degli 11 punti dell’elenco originale

| Punto | Stato | Intervento |
| --- | --- | --- |
| 1. Anagrafica cancella storico | Già corretto | Aggiornamento dei soli nome/cognome |
| 2. Codice admin leggibile | **Irrisolto per compatibilità** | Vedi limite backend sotto |
| 3. Lettura Android riscrive scheda | Già corretto | Ordinamento locale senza scritture |
| 4. Errore avvisi causa crash | Già corretto | Stato errore e retry nel ViewModel del feed/contatore |
| 5. Note diverse fra piattaforme | Corretto in questo intervento | Fonte autorevole `exerciseData/{exerciseKey}/noteUtente` |
| 6. Dialog esercizio perde campi | Corretto in questo intervento | Copia dell’esercizio iniziale, modifica dei soli campi del dialog |
| 7. Consultazione cancella richiesta | Già corretto | Completamento esplicito separato |
| 8. Scadenza iOS anticipata | Già corretto | Confronto dell’istante di fine, etichetta separata |
| 9. Login iOS globale/bloccato | Già corretto | Profilo singolo, errori, cancellazione, timeout e retry |
| 10. Listener iOS duplicati | Già corretto | Handle gestiti e callback vecchie escluse |
| 11. Scheda Android obsoleta | Corretto in questo intervento | Realtime, recupero rete, aggiornamento esplicito e ViewModel condiviso |

## Modifiche di questo intervento

**iOS — note:** la schermata legge e salva la nota della singola parte soltanto
in `users/{code}/exerciseData/{key}/noteUtente`, come Android. Un salvataggio
produce un solo esito e una sola scrittura. Rimuovere una nota non elimina pesi
o altre parti. Le copie preesistenti nella scheda non vengono cancellate o
migrate, ma non inizializzano più il campo note. Entrambi i client già usavano
`exerciseData` per la visualizzazione stabile. Le versioni precedenti continuano
a trovare percorsi e formati invariati e possono continuare le proprie scritture.

**Android admin — esercizi:** `EsercizioDialog` usa `initialExercise.copy(...)`.
Conserva `noteUtente`, `weightLogs`, `priorita` e `ordine`, assenti dal dialog.
Gli editor specifici di giorno, gruppo ed esercizio ora conservano uno snapshot
locale escluso da Firebase e salvano con transazioni a confronto tra originale,
modifica locale e stato corrente. Modifiche indipendenti, note, storico e campi
sconosciuti vengono conservati; modifiche incompatibili mostrano un conflitto e
non scrivono. Anche le creazioni di questi elementi sono atomiche.

**Entrambi i client — chiavi dello storico:** la chiave di `exerciseData` usa ora
normalizzazione indipendente dalla lingua e un hash FNV-1a stabile quando il nome
non produce caratteri ASCII. iOS e Android generano lo stesso valore. Se esiste
già una chiave prodotta dall'algoritmo precedente, viene riutilizzata senza
migrare o duplicare i dati. Percorsi e struttura Firebase restano invariati.

**Android utente — scheda:** un solo ViewModel, fornito da `ContentScreen`,
alimenta Home → giorno → esercizio. Le sottoscrizioni a scheda, nome e stato
connessione vengono sostituite al refresh e rimosse al cambio codice o `onCleared`.
Il listener si registra anche offline e riceve i dati al ritorno della rete.
Errori di lettura terminano il caricamento e mostrano un messaggio con il pulsante
`Aggiorna`. Eliminazioni remote cancellano anche la cache della scheda.
La cache locale è associata al codice; quella legacy senza etichetta viene adottata
una volta per il codice già autenticato (il logout legacy svuota le preferenze).
Nessun cambiamento a percorsi, tipi o scritture Firebase dovuto al caricamento.

## Verifiche e riproduzione

- `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:connectedDebugAndroidTest --console=plain`
- Da `firebase-compatibility`: `npm run test:android-repository` e `npm test`.
- iOS: `python3 Tests/run_note_tests.py`, `python3 Tests/run_lifecycle_tests.py`,
  `python3 Tests/run_model_tests.py`, build Debug Simulator e Release dispositivo
  con `CODE_SIGNING_ALLOWED=NO`.
- Con un iOS Simulator avviato, da `firebase-compatibility`:
  `npm run test:ios-sdk` usa Auth e RTDB emulator con dati fittizi.

Esiti dell'aggiornamento: **30 test unitari Android**, **11 prove SDK/RTDB Android**
e **17 test di compatibilità RTDB/Storage** superati. Tutti e tre i runner iOS
superati, inclusi 6 gruppi sulle scadenze; anche l'host iOS con SDK Firebase reale
ha superato login, retry, cambio utente, realtime e conservazione delle note.
Gli avvisi di compilazione preesistenti non sono stati oggetto di refactoring.

La suite SDK locale comprende cinque prove del repository admin e quattro prove
realtime: avvio offline/riconnessione, aggiornamenti da un secondo client,
refresh ripetuti, rilascio del ViewModel, cambio utente, eliminazione remota,
cache legacy e rifiuto di lettura con retry. Usa app Firebase demo e preferenze
isolate. `goOffline()/goOnline()` simulano l’interruzione del collegamento SDK;
non è una prova con modalità aereo o con diversi provider reali.

Il runner configura `adb reverse tcp:19000 tcp:19000` sul solo Android Emulator:
RTDB Emulator annuncia `127.0.0.1` nell’handshake di riconnessione. Il mapping
viene rimosso alla fine se creato dal runner; un mapping preesistente diverso
fa fallire il test. Non vengono modificati endpoint dell’app reale.

Il test Compose esercita il dialog reale e verifica che cambiare il nome preservi
nota, pesi, priorità e ordine. Il runner iOS in memoria esegue le azioni note della
View e il ViewModel reali: scrittura unica, parti distinte, errore/retry,
eliminazione e conservazione dei pesi. Il nuovo host temporaneo verifica inoltre
il vero SDK iOS contro emulatori Auth/RTDB locali, senza includere codice di test
nel target di produzione.
Non sono state effettuate nuove verifiche visive manuali di queste schermate.

## Limite backend e passi successivi

Il punto 2 non è risolvibile mantenendo contemporaneamente tutti i vincoli
attuali: le versioni installate devono poter leggere `/fausto` e l’admin legacy
scrive con autenticazione anonima senza un ruolo verificato. Negare quelle
operazioni bloccherebbe i client; aggiungere soltanto un nuovo percorso/server
non revoca l’accesso pubblico esistente. Le esposizioni delle regole documentate
in `ROLLOUT.md` restano irrisolte. Non è stato implementato un nuovo sistema di
accesso, né modificato Auth, regole, Storage o dati di produzione.

Prima di un rilascio facoltativo restano la verifica dei binari già distribuiti e
l'assegnazione di build/versioni appropriate. Nessuna pubblicazione negli store
è inclusa in questo intervento.
