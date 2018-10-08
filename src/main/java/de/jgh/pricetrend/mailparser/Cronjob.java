package de.jgh.pricetrend.mailparser;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class Cronjob {

    private MailService mailService;
    private ParserService parserService;
    private RawEntryRepository rawEntryRepository;
    private ProcessedEntryRepository processedEntryRepository;
    private MailRepository mailRepository;
    private JobRepository jobRepository;
    private DetailEntryRepository detailEntryRepository;

    public Cronjob(MailService mailService, ParserService parserService, RawEntryRepository rawEntryRepository, ProcessedEntryRepository processedEntryRepository, MailRepository mailRepository, JobRepository jobRepository, DetailEntryRepository detailEntryRepository) {
        this.mailService = mailService;
        this.parserService = parserService;
        this.rawEntryRepository = rawEntryRepository;
        this.processedEntryRepository = processedEntryRepository;
        this.mailRepository = mailRepository;
        this.jobRepository = jobRepository;
        this.detailEntryRepository = detailEntryRepository;
    }

    @Value("${host}")
    private String host;

    @Value("${port}")
    private String port;

    @Value("${user}")
    private String user;

    @Value("${pw}")
    private String pw;

     @Scheduled(fixedRateString = "3600000")
    public void scheduledTask() throws Exception {
        Job job = jobRepository.save(new Job());
        parseAndProcessMails(job);
        job.setJobStatus(JobStatus.FINISHED);
        jobRepository.save(job);
    }

    private void parseAndProcessMails(Job job) throws Exception {
        job.startJob();

        mailService
                .fetchAndSaveMails(host, port, user, pw);

        List<Mail> mailsToBeProcessed = mailRepository
                .findByProcessed(false);

        job.setProcessedEntries(mailsToBeProcessed.size());
        jobRepository.save(job);

        mailsToBeProcessed
                .stream()
                .forEach(mail -> {
                    mail.setProcessed(true);
                    mailRepository.save(mail);
                    List<RawEntry> rawEntries = parserService.parseMail(mail);
                    rawEntryRepository.saveAll(rawEntries);
                });

        rawEntryRepository.findAll()
                .stream()
                .forEach(baseEntry -> processedEntryRepository.save(new ProcessedEntry(baseEntry)));

        jobRepository.save(job.finishJob());
    }

    @Scheduled(fixedRateString = "86400000")
    public void fetchAndProcessDetailEntries() {
        rawEntryRepository
                .findAll()
                .stream()
                .forEach(this::fetchDetailEntry);

        DetailEntry detailEntry = detailEntryRepository.findTopByProcessed(false);
        processDetailEntryRecursive(detailEntry);
    }

    private void processDetailEntryRecursive(DetailEntry detailEntry) {
        parserService.parseAndProcessDetailEntry(detailEntry);
        DetailEntry next = detailEntryRepository.findTopByProcessed(false);
        if (next != null) {
            processDetailEntryRecursive(next);
        }
    }

    private void fetchDetailEntry(RawEntry rawEntry) {
        try {
            parserService.fetchDetailEntry(rawEntry.getLink());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
