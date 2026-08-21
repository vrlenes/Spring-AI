package tr.gov.karatay.asistan.chat.dto;

// RAG cevabinin, kullanilan kaynak metinlerine gercekten sadik kalip
// kalmadigini kontrol eden ayri bir LLM cagrisinin sonucu (bkz.
// KaynakDogrulamaService). not: dogrulandi=false ise hangi kismin
// desteksiz/uydurma oldugunu kisaca aciklar, dogrulandi=true ise null.
public record KaynakDogrulamaSonucu(boolean dogrulandi, String not) {
}
