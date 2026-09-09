from pathlib import Path

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    (ROOT / path).write_text(text, encoding='utf-8')

def replace_once(text, old, new, label):
    if old not in text:
        raise RuntimeError(f'Marker not found: {label}')
    return text.replace(old, new, 1)

# Add the next strict translation batch. Static UI strings continue to pass through
# UiFactory/I18n, while dynamic sentences are explicitly bilingual in the source.
i18n_path = 'src/main/java/it/casiraghi/swiftbat/ui/I18n.java'
i18n = read(i18n_path)
marker = '        // STRICT_I18N_BATCH_1\n'
entries = [
    ('Catalogo in caricamento…', 'Loading catalog…'),
    ('Apri Data Product', 'Open Data Product'),
    ('Fonte ufficiale ↗', 'Official source ↗'),
    ('indicatore descrittivo', 'descriptive indicator'),
    ('bin con FRACEXP ≈ 1', 'bins with FRACEXP ≈ 1'),
    ('z cosmologico · valore BAT', 'cosmological z · BAT value'),
    ('Totale FITS 15–350 keV', 'FITS total 15–350 keV'),
    ('±20 s dal trigger', '±20 s from trigger'),
    ('±60 s dal trigger', '±60 s from trigger'),
    ('±120 s dal trigger', '±120 s from trigger'),
    ('t = 0 indica il trigger', 't = 0 marks the trigger'),
    ('Vista 3D non disponibile', '3D view unavailable'),
    ('Per costruire il paesaggio tempo–energia servono le quattro bande del file ASCII.', 'The four ASCII energy bands are required to build the time–energy landscape.'),
    ('Keyword', 'Keyword'),
    ('Tutte le bande', 'All bands'),
    ('Non è ancora disponibile una spiegazione specifica per questa voce.', 'A specific explanation for this field is not available yet.'),
    ('Perché serve', 'Why it matters'),
    ('Attenzione', 'Caution'),
    ('Voce FITS', 'FITS field'),
    ('Commento FITS', 'FITS comment'),
    ('Esporta il grafico', 'Export chart'),
    ('Esporta FITS e metadati in Excel', 'Export FITS and metadata to Excel'),
    ('Il trigger è il momento zero', 'The trigger is time zero'),
    ('Il rate descrive l’intensità nel tempo', 'Rate describes intensity over time'),
    ('Le energie sono separate in quattro bande', 'Energy is separated into four bands'),
    ('ERROR e FRACEXP controllano l’affidabilità', 'ERROR and FRACEXP describe data reliability'),
    ('Il FITS è più di una tabella', 'A FITS file is more than a table'),
    ('Gli indicatori presenti nell’app sono descrittivi.', 'The indicators in the app are descriptive.'),
    ('Questa pagina collega le parole tecniche ai dati che stai osservando. Non devi memorizzare tutto: usa le spiegazioni come legenda ragionata.', 'This page connects technical terms to the data you are viewing. You do not need to memorize everything: use the explanations as a reasoned reference.'),
    ('Lo strumento riconosce un aumento significativo e genera un’allerta. Nei grafici trasformiamo quel momento in t = 0. I dati prima del trigger hanno tempo negativo; quelli dopo hanno tempo positivo.', 'The instrument detects a significant increase and generates an alert. In the charts that instant is set to t = 0. Data before the trigger have negative time; data after it have positive time.'),
    ('Ogni riga corrisponde a un intervallo di un secondo. RATE indica il segnale netto stimato in quell’intervallo. Più è alto, più la curva è intensa in quel momento.', 'Each row corresponds to a one-second interval. RATE is the estimated net signal in that interval. Higher values indicate a stronger light curve at that time.'),
    ('Il file ASCII divide il segnale in 15–25, 25–50, 50–100 e 100–350 keV. La curva totale 15–350 keV riunisce queste componenti.', 'The ASCII file separates the signal into 15–25, 25–50, 50–100 and 100–350 keV bands. The 15–350 keV total curve combines these components.'),
    ('ERROR esprime l’incertezza statistica del rate. FRACEXP indica quanta parte del secondo è stata realmente esposta: 1 significa bin completo.', 'ERROR is the statistical uncertainty of the rate. FRACEXP indicates how much of the one-second bin was actually exposed: 1 means full exposure.'),
    ('Contiene sia i numeri della curva sia le intestazioni tecniche: strumento, date, identificativi, coordinate, sistema temporale e dettagli di elaborazione.', 'It contains both light-curve values and technical headers: instrument, dates, identifiers, coordinates, time system and processing details.'),
    ('La vista 3D, il picco, la media, la deviazione standard e la durezza proxy aiutano a esplorare l’evento, ma non sostituiscono il calcolo ufficiale di T90 né una classificazione short/long validata.', 'The 3D view, peak, mean, standard deviation and hardness proxy help explore the event, but they do not replace the official T90 calculation or a validated short/long classification.'),
    ('Seleziona una riga. I metadati sono il registro tecnico del file FITS: descrivono provenienza, tempi, coordinate, struttura e passaggi di elaborazione.', 'Select a row. Metadata are the technical record of the FITS file: they describe provenance, timing, coordinates, structure and processing steps.'),
    ('Un FITS può contenere più sezioni. PRIMARY è l’intestazione generale; RATE è la tabella della curva di luce.', 'A FITS file can contain multiple sections. PRIMARY is the general header; RATE is the light-curve table.'),
    ('È il nome breve del parametro, per esempio OBS_ID, TRIGTIME o TIMEDEL.', 'This is the short parameter name, for example OBS_ID, TRIGTIME or TIMEDEL.'),
    ('È il contenuto associato alla keyword nella riga selezionata: può essere un numero, una data, un identificativo, una stringa o un valore logico.', 'This is the value associated with the keyword in the selected row; it may be a number, date, identifier, string or logical value.'),
    ('È la descrizione scritta dal software che ha prodotto il FITS.', 'This is the description written by the software that produced the FITS file.'),
    ('Questa keyword non è ancora inclusa nel dizionario didattico. Il commento originale del FITS rimane comunque visibile.', 'This keyword is not yet included in the data dictionary. The original FITS comment remains visible.'),
    ('Tempi negativi: prima del trigger. Tempi positivi: dopo il trigger. Il trigger non coincide necessariamente con l’inizio fisico esatto del burst.', 'Negative times are before the trigger; positive times are after it. The trigger does not necessarily coincide with the exact physical onset of the burst.'),
    ('ESC per uscire', 'ESC to exit'),
    ('Mappa non disponibile: ', 'Map unavailable: '),
]
# Normalize curly apostrophes to the actual source spelling where necessary.
entries = [(it.replace('’', "'"), en) for it, en in entries]
existing = i18n
lines = ['        // STRICT_I18N_EXPLORER_BATCH\n']
for it, en in entries:
    escaped_it = it.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
    escaped_en = en.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
    needle = f'put("{escaped_it}",'
    if needle not in existing:
        lines.append(f'        put("{escaped_it}", "{escaped_en}");\n')
i18n = replace_once(i18n, marker, ''.join(lines) + marker, 'i18n marker')
write(i18n_path, i18n)

# Dynamic strings must carry both complete sentences; translating fragments is not reliable.
path = 'src/main/java/it/casiraghi/swiftbat/ui/ExplorerPage.java'
text = read(path)
text = replace_once(text,
'''        catalogCount.setText(entries.size() + (fallback ? " GRB di emergenza" : " GRB nel catalogo online"));''',
'''        I18n.setText(catalogCount,
                entries.size() + (fallback ? " GRB di emergenza" : " GRB nel catalogo online"),
                entries.size() + (fallback ? " fallback GRBs" : " GRBs in the online catalog"));''', 'catalog count')
text = replace_once(text,
'''        Label title = UiFactory.label("Dati non disponibili per " + entry.grbName(), "empty-title");
        Label message = UiFactory.wrappedLabel(
                readableError(error) + "\\n\\nL'evento rimane nel catalogo, ma la struttura online può essere incompleta o diversa da quella standard.",
                "empty-message");''',
'''        Label title = UiFactory.label(
                I18n.dynamic("Dati non disponibili per " + entry.grbName(),
                        "Data unavailable for " + entry.grbName()), "empty-title");
        Label message = UiFactory.wrappedLabel(
                I18n.dynamic(readableError(error)
                                + "\\n\\nL'evento rimane nel catalogo, ma la struttura online può essere incompleta o diversa da quella standard.",
                        readableError(error)
                                + "\\n\\nThe event remains in the catalog, but its online structure may be incomplete or differ from the standard layout."),
                "empty-message");''', 'show error')
text = text.replace('catalogCount.setText(filteredCatalog.size() + " GRB visualizzati");',
                    'I18n.setText(catalogCount, filteredCatalog.size() + " GRB visualizzati", filteredCatalog.size() + " GRBs shown");')
text = text.replace('"Valore completo: " + complete', 'I18n.dynamic("Valore completo: " + complete, "Full value: " + complete)')
write(path, text)

path = 'src/main/java/it/casiraghi/swiftbat/ui/MainView.java'
text = read(path)
text = text.replace('''                    : "Mappa non disponibile: " + error.getMessage();''',
                    '''                    : I18n.dynamic("Mappa non disponibile: " + error.getMessage(),
                            "Map unavailable: " + error.getMessage());''')
write(path, text)

print('Explorer localization batch applied')
