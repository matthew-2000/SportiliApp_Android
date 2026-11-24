# Recupero e widget

## Uso e comportamento

Aprire il timer dall’esercizio e toccare Avvia. Il parser della durata resta quello esistente. La scadenza e la durata sono salvate nelle preferenze locali; il timer continua chiudendo la schermata. La notifica mostra il countdown nativo e le azioni Interrompi e +30 sec. Pausa/Reimposta cancellano l’allarme e la notifica. La schermata aperta ritrova il timer e rileva le azioni eseguite dalla notifica.

Consentire le notifiche quando richiesto. Se negate o se il canale Fine recupero è disattivato, il timer spiega che l’avviso non è attivo. Il collegamento Sveglie e promemoria permette di autorizzare gli allarmi precisi; altrimenti resta un allarme non preciso con un messaggio esplicito sul possibile ritardo. Non sono usati servizi foreground, audio continuo o cicli in background. Il countdown visibile e l’allarme finale sono separati. La notifica del countdown ha una scadenza di visualizzazione, per evitare tempi negativi quando l’avviso finale ritarda.

Aprire un giorno della scheda per sceglierlo, quindi aggiungere il widget «SportiliApp» alla Home dal launcher. Il widget mostra giorno scelto e numero di esercizi. Non stabilisce un’associazione fra giorno della scheda e data. Usa solo una copia locale compatta; non legge Firebase né riceve codice d’accesso o token. Il collegamento include una sessione casuale: un giorno mancante o un vecchio collegamento di un altro utente rimanda a un messaggio e alla scheda.

Logout, cambio codice e cambio identità invalidano snapshot, allarme e notifiche. I nuovi snapshot aggiornano o cancellano il giorno scelto. Le nuove preferenze sono escluse dal backup e dal trasferimento dispositivo. Dopo riavvio/ripartenza del processo e concessione dell’accesso allarmi precisi viene riprogrammata la scadenza salvata, se ancora presente.

## Limiti

Android può ritardare allarmi durante Doze/risparmio energetico, specialmente senza accesso agli allarmi precisi. Anche gli allarmi precisi while-idle hanno limiti di frequenza. Non è garantita una precisione assoluta per una sequenza di recuperi molto brevi. Un arresto forzato impedisce gli avvisi fino alla riapertura; modifiche dei permessi possono cancellare gli allarmi del sistema. Riavvio e revoca permessi sono recuperati al successivo evento/avvio disponibile, non durante un arresto forzato.

Suono e vibrazione dipendono dal canale notifiche e dalle impostazioni di sistema. Il launcher può ritardare l’aggiornamento visivo del widget dopo la cancellazione locale. I collegamenti vecchi restano invalidi anche se il launcher conserva temporaneamente una rappresentazione precedente.

## Verifiche e riproduzione

- Build Debug e test unitari esistenti: passati.
- `NativeSurfaceTest` su Pixel 8, emulatore Android API 34: scadenza, riprogrammazione senza cambiare il termine, vecchio allarme ignorato, +30 secondi, cancellazione, singolo avviso finale, nessuna duplicazione alle letture successive, cambio codice con cancellazione snapshot e rotazione sessione.
- Verificato anche l’allarme realmente consegnato dal sistema senza schermata timer aperta, con accesso agli allarmi precisi. Countdown osservato nella tendina notifiche con app in background.
- Eseguito con POST_NOTIFICATIONS consentito e negato. Non autentica utenti né istanzia repository Firebase; tutte le fixture sono locali.
- Per eseguire i controlli: `./gradlew :app:installDebug :app:installDebugAndroidTest`, poi `adb shell am instrument -w -e class com.matthew.sportiliapp.nativefeatures.NativeSurfaceTest com.matthew.sportiliapp.test/androidx.test.runner.AndroidJUnitRunner`. Impostare il permesso notifiche nell’emulatore prima di ogni variante.
- `NativeSurfacePreviewActivity` è disponibile solo nella build Debug per verificare il timer e preparare un giorno locale per il widget. Alla chiusura cancella la fixture e ripristina il codice precedente. Non avvia repository o letture/scritture Firebase.
- Da verificare su dispositivi fisici: suono/vibrazione, Doze prolungato, limiti dei diversi produttori, riavvio completo e revoca accesso allarmi, gallerie/ridimensionamento dei launcher e link con schede reali. L’emulatore non prova queste garanzie.

## Fonti ufficiali

- [Countdown nelle notifiche](https://developer.android.com/reference/android/app/Notification.Builder)
- [Allarmi](https://developer.android.com/develop/background-work/services/alarms)
- [Accesso agli allarmi precisi](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Permesso notifiche](https://developer.android.com/develop/ui/views/notifications/notification-permission)
- [Widget](https://developer.android.com/develop/ui/views/appwidgets/overview)

## Revisione UI widget e notifiche

- Widget con palette chiara/scura dell’app, titolo del giorno, conteggio con singolare/plurale e indicazione «Apri scheda». Lo stato senza dati invita a scegliere un giorno nell’app.
- Layout regolare e compatto: su Android 12+ il launcher sceglie il RemoteViews adatto allo spazio; sulle versioni precedenti viene aggiornato al ridimensionamento. I widget già installati con altezza ridotta continuano a usare il formato compatto. I cambiamenti di configurazione aggiornano i widget.
- Notifiche di sistema con icona dedicata, colore dell’app, nome dell’esercizio e avviso finale più leggibile. Countdown, canali esistenti, permessi, scadenza e azioni Interrompi/+30 sec restano quelli nativi. Il nome dell’esercizio è solo un campo locale aggiuntivo, cancellato al cambio di sessione.
- Versione 2.0.3 (36): build Debug e bundle Release riusciti, 60 test unitari e tre controlli NativeSurfaceTest passati. I controlli strumentali sono stati ripetuti dopo errori di temporizzazione durante i rallentamenti dell’emulatore.
- I tre controlli NativeSurfaceTest passano su Pixel 8 / API 34: rendering reale RemoteViews nei due formati e temi, stato vuoto e cancellazione del giorno precedente, durata, azioni, metadati dell’esercizio e singolo avviso finale. I controlli attendono la pubblicazione asincrona del NotificationManager prima di leggere l’avviso.
- Anteprima Debug con dati fittizi e widget effettivamente aggiunto alla Home del launcher. Screenshot salvati fuori dal repository. Nessun accesso Firebase diretto nei widget e nessuna scrittura su Firebase durante le prove.
- Il layout delle notifiche viene disegnato dal sistema e può variare fra produttori. Suono, vibrazione e comportamento del risparmio energetico richiedono ancora verifica su dispositivi fisici.
