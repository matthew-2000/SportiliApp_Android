# Miglioramenti compatibili con l'accesso tramite codice

## Ambito concordato

Mantenere l'app semplice e conservare l'attuale accesso tramite codice per
utenti e admin. Il piano non prevede nuovi account, accesso email/password,
inviti, archivi separati o servizi aggiuntivi di autenticazione. Non occorrono
un'email dell'amministratore né un canale di consegna inviti per procedere con
le correzioni concordate.

Le app sono in uso: non richiedere aggiornamenti obbligatori né revocare gli
accessi necessari alle versioni già installate, compreso l'admin.

## Passi previsti

1. **Verificare e distribuire le cinque correzioni come aggiornamenti facoltativi.**
   Le correzioni sono in commit separati Android e iOS e non richiedono modifiche
   backend. Prima della distribuzione verificare i flussi interessati nelle app.
   I vecchi admin possono ancora sovrascrivere dati come prima: le correzioni
   locali non riparano i binari già installati. Preferire l'admin aggiornato per
   le operazioni sui dati, senza disabilitare il precedente.

2. **Valutare soltanto ulteriori miglioramenti concreti e compatibili.**
   Mantenere accesso con codice, percorsi e formati Firebase. Prima di modificare
   dati o operazioni condivise, verificare lettori e scrittori di Android, iOS e
   admin. Evitare migrazioni e servizi aggiuntivi non richiesti.

3. **Verificare ogni eventuale modifica alle regole prima di pubblicarla.**
   Eseguire i test di compatibilità sulle regole proposte e i test di sicurezza
   pertinenti alla modifica. Provare vecchie e nuove versioni in un ambiente di
   test: login con codice, riapertura, scheda, note/pesi, avvisi, immagini e
   operazioni admin. Presentare modifiche esatte, risultati e possibilità di
   ripristino prima del deploy. Non applicare restrizioni che blocchino le
   versioni installate, neppure dopo un'ampia adozione degli aggiornamenti.

## Limiti di sicurezza da segnalare

La verifica iniziale ha trovato Realtime Database con `.read: true` e
`.write: true` alla radice, Storage con lettura pubblica e scrittura concessa
a qualsiasi `request.auth != null`, e Auth anonima abilitata.

Il login iOS installato legge `/fausto` e `/users` prima dell'autenticazione.
L'admin Android usa Auth anonima senza un ruolo verificato dal server. Imporre
subito Auth per quelle letture o un ruolo admin per le scritture bloccherebbe
questi flussi. Aggiungere un nodo con regole restrittive sotto una radice che
concede già accesso pubblico non protegge quel nodo.

Queste esposizioni restano irrisolte nel piano attuale. Segnalarle chiaramente
senza introdurre un nuovo sistema di accesso o dichiarare sicuro il backend.
Un eventuale cambiamento di funzionalità richiede una richiesta esplicita del
proprietario.

## Verifiche disponibili e mancanti

La suite locale descritta in [README.md](README.md) verifica i contratti ricavati
dal codice delle versioni di riferimento. Non sostituisce le prove dei binari
distribuiti: prima di un rilascio restano da verificare le app reali.

Questo lavoro di documentazione e test non ha modificato regole, configurazione
Auth o dati di produzione.
