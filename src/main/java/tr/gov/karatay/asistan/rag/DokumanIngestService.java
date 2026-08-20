package tr.gov.karatay.asistan.rag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DokumanIngestService {

    private final VectorStore vectorStore;
    private final DokumanRepository dokumanRepository;
    private final int chunkBoyutu;

    public DokumanIngestService(
            VectorStore vectorStore,
            DokumanRepository dokumanRepository,
            @Value("${asistan.rag.chunk-size}") int chunkBoyutu) {
        this.vectorStore = vectorStore;
        this.dokumanRepository = dokumanRepository;
        this.chunkBoyutu = chunkBoyutu;
    }

    @Transactional
    public Dokuman iceriAktar(MultipartFile dosya, String baslik, String kategori, String mod) {
        List<Document> hamBelgeler;
        try {
            TikaDocumentReader okuyucu = new TikaDocumentReader(dosya.getResource());
            hamBelgeler = okuyucu.get();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "'%s' dosyası okunamadı, desteklenmeyen veya bozuk bir format olabilir.".formatted(dosya.getOriginalFilename()),
                    e);
        }

        TokenTextSplitter bolucu = TokenTextSplitter.builder().withChunkSize(chunkBoyutu).build();
        List<Document> parcalar = bolucu.split(hamBelgeler);

        if (parcalar.isEmpty()) {
            throw new IllegalArgumentException(
                    "'%s' dosyasından metin çıkarılamadı.".formatted(dosya.getOriginalFilename()));
        }

        Dokuman dokuman = new Dokuman();
        dokuman.setDosyaAdi(dosya.getOriginalFilename());
        dokuman.setBaslik(baslik);
        dokuman.setKategori(kategori);
        dokuman.setMod(mod);
        dokuman.setYuklenmeTarihi(LocalDateTime.now());
        dokuman.setChunkSayisi(parcalar.size());
        dokumanRepository.save(dokuman);

        List<Document> metadatali = new ArrayList<>();
        for (int i = 0; i < parcalar.size(); i++) {
            metadatali.add(parcalar.get(i).mutate()
                    .metadata("dokumanId", dokuman.getId().toString())
                    .metadata("baslik", dokuman.getBaslik())
                    .metadata("kategori", dokuman.getKategori() == null ? "" : dokuman.getKategori())
                    .metadata("mod", dokuman.getMod() == null ? "" : dokuman.getMod())
                    .metadata("dosyaAdi", dokuman.getDosyaAdi())
                    .metadata("chunkIndex", i)
                    .build());
        }

        vectorStore.add(metadatali);
        return dokuman;
    }

    @Transactional
    public void sil(Long dokumanId) {
        Filter.Expression filtre = new FilterExpressionBuilder().eq("dokumanId", dokumanId.toString()).build();
        vectorStore.delete(filtre);
        dokumanRepository.deleteById(dokumanId);
    }
}
