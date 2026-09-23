# Transizione Auth e ruoli senza interrompere le app installate

Stato: preparazione locale; nessun deploy, provider attivato, utente creato,
ruolo assegnato o dato di produzione modificato da questo lavoro.

## Vincolo e limite reale

Le app sono in uso. Non richiedere aggiornamenti obbligatori né revocare accessi
che servono alle versioni installate, compreso l'admin.

La verifica iniziale ha trovato Realtime Database con `.read: true` e
`.write: true` alla radice, Storage con lettura pubblica e scrittura concessa
a qualsiasi `request.auth != null`, e Auth anonima abilitata.
Il login controlla codici tramite dati pubblicamente leggibili; non c'è un
legame affidabile tra UID Firebase e codice utente, né un ruolo admin server.

Non è possibile rendere privati gli stessi dati mantenendo la lettura anonima
di `/users` usata dal login iOS installato. Non è possibile distinguere il
vecchio admin da altri utenti anonimi sulla base delle attuali credenziali.
Le esposizioni legacy restano finché quei flussi devono rimanere disponibili.
Non esiste una regola basata sul numero di versione dichiarato dal client
che risolva questo conflitto in modo affidabile.

## Sequenza proposta

1. **Distribuire le cinque correzioni applicative come aggiornamenti facoltativi.**
   Non richiedono modifiche backend. Sono in commit separati nelle due repo;
   gli utenti sulle vecchie versioni conservano gli stessi accessi. I vecchi
   admin possono ancora sovrascrivere dati come prima: le correzioni locali
   non riparano i binari già installati. Preferire l'admin aggiornato per le
   operazioni sui dati, senza disabilitare il precedente.

2. **Preparare un'identità admin attendibile, separata dal codice legacy.**
   Proposta: account Firebase Auth con email verificata, assegnazione iniziale
   dell'UID admin da operatore autorizzato e ruolo tramite custom claim da
   ambiente server attendibile. Non ricavare il ruolo da `/fausto`, dai nomi,
   da `displayName`, da preferenze locali o da record modificabili dai client.
   Prima del provisioning serve identificare l'account dell'amministratore;
   nessun account viene scelto automaticamente perché è collegato alla CLI.

3. **Preparare l'associazione attendibile degli utenti.**
   Il codice attuale è un identificatore pubblico, non una prova di identità.
   Proposta: invito nuovo, monouso, fornito dal trainer attraverso un canale
   separato; il server ne controlla scadenza, utilizzo unico e tentativi prima
   di associare UID e utente. Non elevare automaticamente sessioni anonime
   usando il solo codice pubblico. Definire anche recupero account e cambio
   dispositivo prima di attivare il flusso.

4. **Isolare i dati di autorizzazione.**
   Con le concessioni pubbliche alla radice RTDB, un figlio con accesso negato
   non è privato. Usare un archivio server protetto indipendente da quella
   radice, ad esempio una seconda istanza RTDB con regole chiuse, per inviti e
   associazioni; non è ancora stata creata. Non importare ruoli da dati legacy
   né propagare automaticamente scritture legacy nell'archivio attendibile.
   Questo protegge il nuovo controllo degli accessi, non rende privati i dati
   applicativi ancora esposti dal backend legacy.

5. **Introdurre i nuovi flussi in modo facoltativo nelle nuove app.**
   Conservare login legacy, Auth anonima, schema, immagini e comportamento delle
   versioni installate. Se una risorsa nuova richiede un'identità attendibile,
   un errore di autorizzazione non deve attivare un ripiego pubblico per quella
   risorsa. Il percorso legacy continua ad accedere soltanto ai dati legacy.

6. **Validare una proposta concreta prima di pubblicarla.**
   Eseguire la suite di compatibilità contro ogni futura regola candidata e
   test negativi per accesso ad altri utenti, ruolo autoassegnato, invito
   riutilizzato/scaduto, credenziali mancanti e scritture su campi protetti.
   Provare vecchi e nuovi binari, admin compreso, su ambiente di test isolato:
   primo login, riapertura, scheda, note/pesi, avvisi, immagini e operazioni admin.
   Presentare le modifiche esatte, le prove, il piano di ripristino e
   l'esposizione residua prima del deploy. Nessuna chiusura automatica del
   percorso legacy al raggiungimento di una percentuale di aggiornamenti.

## Cosa manca prima dell'implementazione del nuovo login

- Account da usare per l'admin e procedura di recupero.
- Scelta/conferma del canale con cui consegnare gli inviti agli utenti.
- Progetto dettagliato e test dell'archivio isolato e del servizio di scambio
  inviti; questa repo non contiene ancora quel servizio.
- Disponibilità dei binari delle versioni di riferimento per la verifica
  completa in ambiente di test. La suite attuale verifica i contratti ricavati
  dal codice, non sostituisce questa prova.

La compatibilità totale e la chiusura completa dei dati legacy sono obiettivi
in conflitto con gli attuali client. Qualsiasi futura proposta che prometta
entrambi senza cambiare quel vincolo va respinta.
