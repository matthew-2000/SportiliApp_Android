# Rilasci Android con GitHub Actions e Google Play

## Stato e funzionamento

`Android - Verifica` compila il Debug con una configurazione Firebase fittizia,
esegue i test unitari e verifica i contratti legacy su emulatori locali.
Parte sulle pull request e sui push a `main`, oppure manualmente.
Non avvia l'app sul backend condiviso e non distribuisce regole Firebase.

`Android - Beta Google Play` parte solo manualmente dal branch predefinito.
Riesegue prima tutti i controlli, poi esegue i test Release, firma il bundle
con la **chiave di upload esistente**, conserva l'AAB e lo carica sul solo
canale `internal` tramite Google Play Developer API. Verifica numero e SHA-256
restituiti da Google prima di aggiornare il canale. Conserva la ricevuta con
commit, versione, edit ID e impronta dell'AAB per 30 giorni.

Il job di distribuzione resta disabilitato finché `PLAY_INTERNAL_ENABLED`
non viene impostata a `true`. Non esiste un workflow di produzione.
Il setup locale non equivale ad aver collegato o provato gli account remoti.

## Collegamenti iniziali

1. Pubblicare queste modifiche su GitHub insieme al codice da verificare.
   La toolchain usa Gradle 9.8, SDK 37 e daemon JBR 25, con la versione AGP
   dichiarata nel catalogo Gradle. Eventuali aggiornamenti AGP restano separati.
   Il runner installa Java 25; Gradle rispetta i criteri JBR già nel repository.
2. In GitHub abilitare Actions e creare l'environment `play-internal`, limitato
   al branch predefinito. Le PR non ricevono i segreti di distribuzione.
3. Nel progetto Google Cloud scelto per la pubblicazione abilitare Google Play
   Android Developer API e creare un service account dedicato. Questo non
   modifica Auth, Realtime Database o Storage dell'app.
4. In Play Console invitare quel service account dalla sezione utenti e
   permessi. Limitare l'accesso a `com.matthew.sportiliapp`, con lettura e rilascio
   sui canali di test. Non assegnare permessi di produzione o amministratore.
5. Configurare Workload Identity Federation per GitHub OIDC, con audience
   prevista dal provider. Limitare la condizione al repository Android
   (preferibilmente ID numerici di repository e proprietario), al branch
   predefinito e all'environment `play-internal`. Assegnare al principal
   federato `roles/iam.workloadIdentityUser` sul service account.
   Non creare o caricare una chiave privata JSON permanente.
6. Configurare le variabili e i segreti elencati sotto. La chiave di upload
   deve corrispondere al certificato già registrato in Play Console: non
   generare una nuova chiave e non modificare Play App Signing.
7. Eseguire `Android - Verifica` e controllarne l'esito remoto. Solo dopo
   impostare `PLAY_INTERNAL_ENABLED=true` e avviare la prima Beta.

### Variabili GitHub

| Nome | Dove | Valore |
| --- | --- | --- |
| `PLAY_INTERNAL_ENABLED` | Repository | `true` solo al termine del setup |
| `GOOGLE_PLAY_WIF_PROVIDER` | Environment `play-internal` | `projects/NUMERO/locations/global/workloadIdentityPools/POOL/providers/PROVIDER` |
| `GOOGLE_PLAY_SERVICE_ACCOUNT` | Environment `play-internal` | Email del service account autorizzato su Play |

### Segreti dell'environment `play-internal`

| Nome | Contenuto |
| --- | --- |
| `ANDROID_GOOGLE_SERVICES_JSON` | Configurazione esistente dell'app per `sportiliapp`, JSON completo |
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | Keystore di upload esistente codificato Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Password del keystore |
| `ANDROID_KEY_ALIAS` | Alias della chiave esistente |
| `ANDROID_KEY_PASSWORD` | Password della chiave |

Inserire questi valori nelle impostazioni GitHub, senza copiarli in chat,
file versionati o log. I workflow cancellano dal runner i file di firma al termine.

## Preparare una Beta

1. Fare commit del codice e delle note italiane in `release-notes/`, massimo
   500 caratteri. Non inserire note segnaposto in una release reale.
2. Verificare in Play Console il massimo `versionCode` già usato in tutti
   i canali. Scegliere un intero maggiore; il numero locale 36 non dimostra
   quale sia il massimo sullo store. Non riutilizzare un codice dopo un upload
   riuscito, anche se un passaggio successivo del workflow fallisce.
3. Avviare `Android - Beta Google Play` sul branch predefinito, indicando
   `version_name` (es. `2.0.4`), `version_code` e il percorso completo delle note
   (es. `release-notes/2.0.4-it-IT.txt`). La numerazione è esplicita in questa
   prima configurazione, per evitare collisioni con i rilasci manuali.
4. Controllare l'artifact AAB, i test, la ricevuta e la disponibilità sul canale
   interno. Configurare il gruppo di tester in Play Console e provare l'app.
   Il tempo di disponibilità dipende anche dai controlli Google.

Non eseguire contemporaneamente modifiche di release in Play Console e nella
pipeline: gli edit condividono lo stato dell'app. Se esiste una revisione in
corso, lo script si ferma senza annullarla; non forza un secondo tentativo
con opzioni che cambiano la revisione. Errori di permessi, firma o stato
dell'app vanno risolti prima di rilanciare.

## Approvazione e produzione

Dopo la prova il proprietario approva e promuove **lo stesso versionCode già
testato** dalla Play Console. Nessuna ricompilazione per la produzione.
Controllare note, dichiarazioni e revisione, quindi usare il rilascio graduale
ufficiale quando disponibile e appropriato. L'invio e la promozione restano
manuali fino alla prima Beta riuscita; questa configurazione non realizza
ancora l'automazione completa della produzione con una sola approvazione.

Per modifiche condivise Firebase/admin, verificare anche i client installati:
i test contrattuali non dimostrano da soli la compatibilità di tutti i binari
distribuiti. Questi workflow non cambiano dati, accessi, schema o regole.

## Verifica locale della procedura Play

```sh
python3 -m unittest discover -s ci -p 'test_*.py' -v
SPORTILI_VERSION_NAME=2.0.4 SPORTILI_VERSION_CODE=37 \
  NOTES_FILE=release-notes/2.0.4-it-IT.txt python3 ci/publish_internal.py
```

Il secondo comando richiede un file di note reale e, senza `--publish`,
non effettua richieste a Google. I test usano soltanto un'API simulata.

Fonti ufficiali:
[canali Play](https://developers.google.com/android-publisher/tracks),
[edit e pubblicazione](https://developers.google.com/android-publisher/edits),
[comportamento durante una revisione](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit),
[autenticazione GitHub verso Google](https://github.com/google-github-actions/auth),
[configurazione delle API Play](https://developers.google.com/android-publisher/getting_started).
