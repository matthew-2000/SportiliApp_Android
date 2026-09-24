# Firebase: compatibilità delle versioni installate

Suite locale condivisa da Android, admin e iOS, mantenuta nella repo Android
che contiene l'admin. Non è una configurazione di deploy e non cambia le app.

## Versioni di riferimento

Il proprietario ha indicato le versioni precedenti agli ultimi commit:

| Componente | Versione nel sorgente | Commit di riferimento |
| --- | --- | --- |
| Android e admin | 1.3.4, versionCode 30 | `03701e9` |
| iOS | 1.5.3, build 15 | `c7c91be` nella repo iOS |

I numeri sono ricavati dalle configurazioni dei commit, non dagli store o dai
dispositivi. Le correzioni `c085d28` (Android) e `18d3354` (iOS) non cambiano
login, regole, nomi dei nodi o tipi dei campi Firebase.

## Esecuzione

Prerequisiti: Node.js 22+ compatibile con Firebase CLI e Java 21 sul PATH.
Le dipendenze SDK di test sono fissate in `package-lock.json`; la CLI viene
eseguita con `npx -y firebase-tools@latest`.

Da questa cartella:

```sh
npm ci --ignore-scripts
npm test
```

Su questa macchina è stato verificato anche:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
PATH='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin' \
npm test
```

Il comando usa soltanto gli emulatori su `127.0.0.1`, un progetto
`demo-sportili-compat` e dati inventati. Non richiede credenziali Firebase e non
carica configurazioni SDK di produzione. I test caricano esplicitamente le
regole negli emulatori; il file `firebase.emulators.json` contiene solo le
impostazioni degli emulatori, senza destinazioni di deploy.

Per valutare una futura proposta senza pubblicarla:

```sh
SPORTILI_DATABASE_RULES=/percorso/assoluto/candidate.database.rules.json \
SPORTILI_STORAGE_RULES=/percorso/assoluto/candidate.storage.rules \
npm test
```

È possibile specificare una sola variabile. Il file non specificato resta lo
snapshot iniziale. Un percorso inesistente o regole non valide fanno fallire
i test; non è previsto un ripiego su regole aperte.

## Cosa verificano i test

### Prove native Android

Con un Android Emulator avviato, dalla root della repository:

```sh
./gradlew :app:connectedDebugAndroidTest --console=plain
```

Le prove di interfaccia usano i componenti reali con dati in memoria: salvataggio
ordinario, completamento esplicito della richiesta, apertura giorno, durata non
valida, pulsanti disabilitati durante il salvataggio e recupero degli avvisi
dopo un errore. Non accedono al database di produzione.

Per verificare anche `FirebaseRepositoryImpl` con il vero SDK Android e RTDB
locale, da questa cartella, con Java 21 sul PATH:

```sh
npm run test:android-repository
```

Questo comando avvia RTDB sul computer, carica regole fittizie e avvia
`FirebaseRepositoryEmulatorTest` e `SchedaRealtimeEmulatorTest` con l'abilitazione esplicita del test locale.
Richiede un Android Emulator, non un telefono fisico: usa `10.0.2.2:19000` e
un'istanza Firebase separata con project ID `demo-sportili-compat`. Le nove prove
verificano conservazione di scheda/storico/campi sconosciuti, rifiuto di scrittura,
creazione e salvataggio concorrenti con due client, modifiche obsolete e conflitti.
Coprono anche avvio offline, riconnessione, cache, cambio utente e rilascio.
Nella suite nativa ordinaria queste nove prove sono saltate
finché non viene passato l'argomento `sportiliLocalDatabase=1` dal comando sopra.
Il runner configura e rimuove un tunnel `adb reverse` locale per permettere
anche le riconnessioni all’host annunciato dall’emulatore.
I test non hanno un ripiego sul backend reale.

### Contratti delle regole Firebase

`compatibility.test.mjs` verifica 12 scenari di accesso osservati nel codice:

- iOS legacy legge `/fausto` e tutto `/users` prima di `signInAnonymously`
  (la versione corretta legge soltanto `/users/{code}`; il contratto legacy resta coperto);
- Android legge `/users/{code}` e `/fausto` con autenticazione anonima;
- entrambi leggono scheda, profilo, storico, catalogo e avvisi;
- entrambi salvano/modificano/eliminano note e pesi in `exerciseData` e scrivono
  `scheda/cambioRichiesto`; iOS contiene anche scritture di note nella scheda;
- Android 1.3.4 può riscrivere tutta la scheda durante la normalizzazione;
- l'admin anonimo legge tutti gli utenti, crea/sostituisce/elimina utenti,
  schede, giorni, gruppi, esercizi e avvisi, e gestisce le segnalazioni;
- l'aggiornamento parziale del nome introdotto dalla correzione conserva
  scheda e storico;
- la lettura Storage resta possibile senza Auth e con Auth anonima.

Riferimenti Android: `LoginScreen.kt`, `newadmin/utils/AdminAccessValidator.kt`,
`newadmin/data/FirebaseRepositoryImpl.kt`, `model/SchedaViewModel.kt` sotto
`app/src/main/java/com/matthew/sportiliapp/`.
Riferimenti iOS: `SportiliApp/LoginView.swift`, `Model/ExerciseDetailViewModel.swift`,
`Model/Scheda.swift`, `User/EsercizioView.swift`, `ImageLoader.swift`.

`security-boundaries.test.mjs` contiene 5 dimostrazioni separate, sempre sullo
snapshot iniziale o su regole fittizie. Mostra che le regole pubblicate
permettono letture/scritture RTDB non autenticate e scritture Storage a qualsiasi
utente anonimo; verifica inoltre perché tre scorciatoie sono inaccettabili:
richiedere subito Auth, richiedere subito un ruolo admin, aggiungere un nodo
"privato" sotto una radice che concede già accesso pubblico.

**Test verdi significano compatibilità degli scenari coperti, non sicurezza.**
I 5 test dimostrativi non valutano la sicurezza delle eventuali regole candidate.
Prima di un deploy serve anche una suite di autorizzazione negativa specifica
per la proposta, oltre alla verifica con le applicazioni reali.

## Limiti

- Sono richieste SDK JavaScript equivalenti ai percorsi nel sorgente, non
  esecuzioni delle app native distribuite. Auth è simulata, non viene provato
  il login reale né il ripristino della sessione su dispositivo.
- Storage verifica le regole di lettura tramite metadata, non il download di
  immagini dalle app, i token presenti negli URL Android o la cache/CDN.
- Non sono coperti App Check, provider Auth di produzione, modalità offline,
  ogni possibile forma dei dati esistenti o versioni precedenti ai riferimenti.
- Le fixture delle regole riproducono la verifica iniziale del backend condiviso;
  non sono regole sicure consigliate e non vanno pubblicate come una correzione.

## Verifica eseguita il 23 settembre 2026

Con Node.js 26.0.0 e Java 21.0.10: 17 test superati sullo snapshot iniziale.
Passando tramite `SPORTILI_DATABASE_RULES` regole fittizie che richiedono Auth,
la suite restituisce un errore e fallisce esattamente il test del login iOS
(16 passati, 1 fallito atteso). Il controllo intercetta quindi quella proposta
incompatibile prima del deploy. Nessuna build nativa è stata eseguita per
questa suite.

Fonti Firebase: [test delle regole](https://firebase.google.com/docs/rules/unit-tests),
[Realtime Database Emulator](https://firebase.google.com/docs/emulator-suite/connect_rtdb).
Il piano dei miglioramenti compatibili con l'accesso tramite codice e i relativi
vincoli sono in [ROLLOUT.md](ROLLOUT.md).
Gli esiti delle prove native e delle build sono in [VERIFICATION.md](VERIFICATION.md).
