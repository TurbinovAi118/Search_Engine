package engine.utils;

import engine.services.LemmaService;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.stream.Collectors;

public class SnippetParser {

    private final LemmaService lemmaService;

    public SnippetParser(LemmaService lemmaService) {
        this.lemmaService = lemmaService;
    }

    public Map<String, Integer> defineQueryWordsInLemmas(String[] queryList, Map<String, Integer> sortedQueryLemmas) {
        Map<String, Integer> queryWordsToLemmas = new HashMap<>();
        for (String queryWord : queryList) {

            Map<String, Integer> lemmasInQuery = new HashMap<>();
            lemmaService.getNormalForms(queryWord).forEach(word -> {
                if (sortedQueryLemmas.containsKey(word)) {
                    lemmasInQuery.put(word, sortedQueryLemmas.get(word));
                }
            });
            if (lemmasInQuery.size() > 0)
                queryWordsToLemmas.put(queryWord, lemmasInQuery.values().stream().reduce(Integer::max).get());
        }
        return queryWordsToLemmas;
    }

    public String parseSnippet(Document doc, Map<String, Integer> queryLemmas, String query) {
        String maxRelevanceQueryWord = queryLemmas.entrySet().stream()
                .min(Map.Entry.comparingByValue(Comparator.reverseOrder())).get().getKey();

        Elements foundElements = doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())).size() > 0 ?
                doc.body().getElementsContainingOwnText(String.join(" ", queryLemmas.keySet())) :
                doc.body().getElementsContainingOwnText(maxRelevanceQueryWord);

        String elementText;
        String snippet = "";
        for (Element element : foundElements) {
            elementText = element.text();
            try {
                snippet = markup(elementText, query);
            } catch (Exception e) {
                continue;
            }
            if (!snippet.isEmpty())
                break;
        }

        if (snippet.isEmpty()) {
            return null;
        }

        String finalSnippet = getSnippetArray(snippet);

        return "..." + finalSnippet + "...";
    }

    private String markup(String text, String query) {
        String regex = "[^а-яА-Яa-zA-Z\s]";

        String[] textWords = text.split(" ");
        List<String> queryWords = Arrays.asList(query.split(" "));

        List<List<String>> textLemmas = new ArrayList<>();
        List<List<String>> queryLemmas = new ArrayList<>();
        List<String> textList = new ArrayList<>();

        for (String word : textWords) {
            textList.add(word);
            try {
                textLemmas.add(lemmaService.getNormalForms(word.replaceAll(regex, "").toLowerCase(Locale.ROOT)));
            } catch (Exception e) {
                textLemmas.add(new ArrayList<>());
            }
        }

        queryWords.forEach(word -> queryLemmas.add(lemmaService.getNormalForms(word)));

        StringJoiner queryLemmasJoiner = new StringJoiner(" ");
        queryLemmas.forEach(list -> queryLemmasJoiner.add(String.join(" ", list)));

        int firstWordIndex = textLemmas.indexOf(textLemmas.stream().filter(list -> list.containsAll(queryLemmas.get(0)))
                .collect(Collectors.toList()).get(0));
        List<List<String>> textLemmasSub;
        boolean forWhile = true;
        if (firstWordIndex == -1)
            return "";

        while (forWhile) {
            textLemmasSub = textLemmas.subList(firstWordIndex, textLemmas.size());
            StringJoiner textChecker = new StringJoiner(" ");
            textLemmasSub.forEach(list -> textChecker.add(String.join(" ", list)));
            if (textChecker.toString().startsWith(queryLemmasJoiner.toString()))
                forWhile = false;
            else {
                textLemmasSub = textLemmas.subList(firstWordIndex + 1, textLemmas.size());
                int common = (!textLemmasSub.contains(queryLemmas.get(0))) ? -1 : textLemmasSub.indexOf(queryLemmas.get(0));
                firstWordIndex += (common == -1) ? -(firstWordIndex + 1) : common + 1;
            }
            if (firstWordIndex == -1)
                return "";
        }
        for (int i = 0; i < queryWords.size(); i++) {
            textList.set(firstWordIndex + i, "<b>" + textList.get(firstWordIndex + i) + "</b>");
        }

        return String.join(" ", textList);
    }

    private static String getSnippetArray(String elementText) {
        int indexOfCommon = elementText.toLowerCase(Locale.ROOT).indexOf("<b>");
        int lastIndexOfCommon = elementText.toLowerCase(Locale.ROOT).lastIndexOf("</b>") + 4;
        if (indexOfCommon == -1) {
            return "";
        }
        int indexOfFirstComma = elementText.substring(0, indexOfCommon).lastIndexOf(",");
        int indexOfLastComma = elementText.substring(lastIndexOfCommon).indexOf(",");

        String startOfSnippet = elementText.substring(indexOfFirstComma == -1 ? 0 : indexOfFirstComma + 1, lastIndexOfCommon);
        String endOfSnippet = elementText.substring(lastIndexOfCommon, (indexOfLastComma == -1 ? elementText.length() : lastIndexOfCommon + indexOfLastComma));
        String text = startOfSnippet + endOfSnippet;
        text = text.trim();

        return text;
    }
}
