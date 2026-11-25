# Produzione Android con GitHub Actions

## Flusso

Avviare `Android - Produzione Google Play` da `main`: controlli automatici,
compilazione/firma AAB, conservazione di bundle e piano, **una approvazione**,
invio dello stesso AAB direttamente al canale `production` tramite API Google.
Nessun passaggio obbligatorio su canali di test. Il riepilogo mostra commit,
versione, note, SHA-256 e percentuale. Il job finale non ricompila.
Firma tramite la chiave di upload esistente e Play App Signing attuale.
Nessuna modifica a Firebase, dati, regole o flussi delle versioni installate.

## Setup una sola volta

1. Unire la PR e verificare `Android - Verifica` su GitHub. I controlli usano
   Firebase fittizio ed emulatori locali.
2. Creare l'environment `android-signing`, limitato a `main`, senza approvazione
   manuale. Inserire qui i cinque segreti della tabella.
3. Creare `google-play-production`, limitato a `main`, con Matteo come
   **required reviewer**. Consentire l'approvazione della propria esecuzione
   se Matteo avvia il workflow; disabilitare il bypass delle regole.
   Il solo campo `environment:` nel YAML NON crea la regola di approvazione.
4. Abilitare Google Play Android Developer API nel progetto Cloud scelto e
   autorizzare il service account in Play Console per la sola app
   `com.matthew.sportiliapp`: lettura e **rilascio in produzione**. I precedenti
   permessi limitati ai test non bastano.
5. Configurare GitHub OIDC/Workload Identity Federation e il ruolo
   `roles/iam.workloadIdentityUser` sul service account. Limitare il provider a
   repository ID `809730971`, owner ID `55196582`, `refs/heads/main` e subject
   `repo:matthew-2000/SportiliApp_Android:environment:google-play-production`.
   Il subject precedente `play-internal` non serve alla produzione.
6. Inserire le due variabili Google nell'environment `google-play-production`.
7. In Play Console controllare **Pubblicazione gestita**: per il rilascio
   automatico dopo la revisione deve essere disattivata. Se attiva, Google
   trattiene le modifiche approvate e richiede un ulteriore comando dalla
   console; il commit API non supera quel blocco. Non cambiarla mentre ci
   sono modifiche approvate in attesa senza valutarne l'effetto.
8. Solo con account e reviewer pronti creare la variabile di repository
   `PLAY_PRODUCTION_ENABLED=true`.

| Secret in `android-signing` | Contenuto |
| --- | --- |
| `ANDROID_GOOGLE_SERVICES_JSON` | JSON attuale di sportiliapp per Android |
| `ANDROID_UPLOAD_KEYSTORE_BASE64` | Keystore di upload esistente in Base64 |
| `ANDROID_KEYSTORE_PASSWORD` | Password del keystore |
| `ANDROID_KEY_ALIAS` | Alias esistente |
| `ANDROID_KEY_PASSWORD` | Password della chiave |

| Variabile in `google-play-production` | Contenuto |
| --- | --- |
| `GOOGLE_PLAY_WIF_PROVIDER` | projects/NUMERO/locations/global/workloadIdentityPools/POOL/providers/PROVIDER |
| `GOOGLE_PLAY_SERVICE_ACCOUNT` | Email del service account autorizzato su Play |

`PLAY_INTERNAL_ENABLED` e `play-internal` non servono più. Non generare una
nuova chiave di upload o una chiave JSON permanente del service account.
Non inserire segreti in chat, file Git o log.

## Ogni rilascio

1. Completare eventuali dichiarazioni Google: scheda store, Data safety,
   accesso per la revisione e contratti, senza inventare risposte.
2. Fare commit su `main` delle modifiche e delle note reali in `release-notes/`.
3. Actions > `Android - Produzione Google Play` > Run workflow, su `main`.
   Inserire `version_name`, un `version_code` sopra tutti quelli già caricati,
   `notes_file` e `rollout_percent`.
4. Dopo build e controlli, leggere il riepilogo e approvare il deployment.
5. La pipeline invia l'AAB in produzione. La ricevuta attesta l'invio,
   non l'approvazione Google o la disponibilità pubblica.

`rollout_percent=100` (predefinito) richiede il rilascio completo dopo revisione.
Con 1–99 Google avvia un rollout a quella percentuale: questa pipeline NON
lo aumenta automaticamente fino al 100%.

Lo script blocca rollout già in corso/in pausa e non annulla revisioni.
Non lavorare simultaneamente sulle release in Play Console. Dopo un errore
verificare lo stato prima di rilanciare: un upload riuscito può consumare il
versionCode anche se il commit fallisce. Nessun retry automatico delle scritture.

## Verifica e limiti

`python3 -m unittest discover -s ci -p 'test_*.py' -v`

API simulate: rilascio completo/graduale, SHA/versione, commit e artifact,
rollout esistenti, errori e pulizia degli edit. Firma reale e primo upload
richiedono il setup degli account. Numeri/versioni restano espliciti.
Per cambi Firebase condivisi restano necessarie le verifiche sui client
installati previste nelle istruzioni del progetto.

Fonti: [canali](https://developers.google.com/android-publisher/tracks),
[commit](https://developers.google.com/android-publisher/api-ref/rest/v3/edits/commit),
[OIDC](https://github.com/google-github-actions/auth),
[pubblicazione gestita](https://support.google.com/googleplay/android-developer/answer/9859654).
