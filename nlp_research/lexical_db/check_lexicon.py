import json
data = json.load(open('datasets/processed/lexical_database/nagamese_lexicon.json', 'r', encoding='utf-8'))
total = len(data)
etym_counts = {}
for e in data:
    et = e.get('etymology_origin', 'Unknown')
    etym_counts[et] = etym_counts.get(et, 0) + 1
print('Total Entries:', total)
print('Etymology Breakdown:')
for k, v in sorted(etym_counts.items(), key=lambda x: -x[1]):
    print('  ' + k + ': ' + str(v))
print('\nTop 10 by frequency:')
for e in data[:10]:
    print('  ' + e['id'] + ': ' + e['lemma'] + ' (' + str(e['frequency_count']) + ')')
print('\nLast Entry: ' + data[-1]['id'] + ' - ' + data[-1]['lemma'])
