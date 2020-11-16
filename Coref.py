import spacy
nlp = spacy.load('en_coref_lg')

examples = [
    u'My sister has a dog and she loves him.',
    u'My sister has a dog and she loves him. He is cute.',
    u'My sister has a dog and she loves her.',
    u'My brother has a dog and he loves her.',
    u'Mary and Julie are sisters. They love chocolates.',
    u'John and Mary are neighbours. She admires him because he works hard.',
    u'X and Y are neighbours. She admires him because he works hard.',
    u'The dog chased the cat. But it escaped.',
]

def printMentions(doc):
    print('\nAll the "mentions" in the given text:')
    for cluster in doc._.coref_clusters:
        print(cluster.mentions)

def printPronounReferences(doc):
    print('\nPronouns and their references:')
    for token in doc:
        if token.pos_ == 'PRON' and token._.in_coref:
            for cluster in token._.coref_clusters:
                print(token.text + " => " + cluster.main.text)

def processDoc(text):
    doc = nlp(text)
    if doc._.has_coref:
        print("Given text: " + text)
        printMentions(doc)
        printPronounReferences(doc)

if __name__ == "__main__":
    processDoc(examples[8])