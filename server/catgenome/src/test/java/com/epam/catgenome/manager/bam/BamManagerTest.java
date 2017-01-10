/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.manager.bam;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.epam.catgenome.controller.util.MultipartFileSender;
import com.epam.catgenome.controller.util.UrlTestingUtils;
import com.epam.catgenome.controller.vo.registration.IndexedFileRegistrationRequest;
import com.epam.catgenome.controller.vo.registration.ReferenceRegistrationRequest;
import com.epam.catgenome.dao.BiologicalDataItemDao;
import com.epam.catgenome.entity.BiologicalDataItem;
import com.epam.catgenome.entity.BiologicalDataItemResourceType;
import com.epam.catgenome.entity.bam.BamFile;
import com.epam.catgenome.entity.bam.BamQueryOption;
import com.epam.catgenome.entity.bam.BamTrack;
import com.epam.catgenome.entity.bam.Read;
import com.epam.catgenome.entity.bam.TrackDirectionType;
import com.epam.catgenome.entity.bucket.Bucket;
import com.epam.catgenome.entity.reference.Chromosome;
import com.epam.catgenome.entity.reference.Reference;
import com.epam.catgenome.entity.reference.Sequence;
import com.epam.catgenome.entity.track.Track;
import com.epam.catgenome.manager.bucket.BucketManager;
import com.epam.catgenome.manager.reference.ReferenceManager;

/**
 * Source:      BamManagerTest.java
 * Created:     12/3/2015
 * Project:     CATGenome Browser
 * Make:        IntelliJ IDEA 14.1.4, JDK 1.8
 *
 * @author Semen_Dmitriev
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class BamManagerTest {
    private Logger logger = LoggerFactory.getLogger(BamManagerTest.class);

    @Autowired
    ApplicationContext context;

    @Autowired
    private BucketManager bucketManager;

    @Autowired
    private BamFileManager bamFileManager;

    @Autowired
    private ReferenceManager referenceManager;

    @Autowired
    private BiologicalDataItemDao biologicalDataItemDao;

    @Autowired
    private BamManager bamManager;

    private static final String TEST_NSAME = "BIG";
    private static final int TEST_START_INDEX_SMALL_RANGE = 12589188;
    private static final int TEST_START_INDEX_MEDIUM_RANGE = 12589188;
    private static final int TEST_START_INDEX_LARGE_RANGE = 12582200;
    private static final int TEST_END_INDEX_SMALL_RANGE = 12589228;
    private static final int TEST_END_INDEX_MEDIUM_RANGE = 12589228;
    private static final int TEST_END_INDEX_LARGE_RANGE = 12589228;
    private static final double SCALE_FACTOR_SMALL = 1.0;
    private static final double SCALE_FACTOR_LARGE = 0.0000625;
    private static final double SCALE_FACTOR_MEDIUM = 0.0105;
    private static final int TEST_FRAME_SIZE = 30;
    private static final int LARGE_FRAME_SIZE = 12589188;
    private static final int TEST_COUNT = 30;
    private static final int LARGE_TEST_COUNT = 10000000;

    private Resource resource;
    private String chromosomeName = "X";
    private Reference testReference;
    private Chromosome testChromosome;


    @Value("${s3.bucket.test.name}")
    private String s3BucketName;
    @Value("${s3.access.test.key}")
    private String s3AccessKey;
    @Value("${s3.secret.test.key}")
    private String s3SecretKey;
    @Value("${s3.file.path}")
    private String s3FilePath;
    @Value("${s3.index.path}")
    private String s3IndexPath;


    @Value("${hdfs.file.path}")
    private String hdfsFilePath;
    @Value("${hdfs.index.path}")
    private String hdfsIndexPath;

    @Before
    public void setup() throws IOException {

        resource = context.getResource("classpath:templates");
        File fastaFile = new File(resource.getFile().getAbsolutePath() + "//dm606.X.fa");

        ReferenceRegistrationRequest request = new ReferenceRegistrationRequest();
        request.setName(TEST_NSAME);
        request.setPath(fastaFile.getPath());

        testReference = referenceManager.registerGenome(request);
        List<Chromosome> chromosomeList = testReference.getChromosomes();
        for (Chromosome chromosome : chromosomeList) {
            if (chromosome.getName().equals(chromosomeName)) {
                testChromosome = chromosome;
                break;
            }
        }
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void saveBamTest() throws IOException, InterruptedException {
        final String path = resource.getFile().getAbsolutePath() + "//agnX1.09-28.trim.dm606.realign.bam";
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + ".bai");
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);
        Assert.notNull(bamFile);
        final BamFile loadBamFile = bamFileManager.loadBamFile(bamFile.getId());
        Assert.notNull(loadBamFile);
        Assert.isTrue(bamFile.getId().equals(loadBamFile.getId()));
        Assert.isTrue(bamFile.getName().equals(loadBamFile.getName()));
        Assert.isTrue(bamFile.getCreatedBy().equals(loadBamFile.getCreatedBy()));
        Assert.isTrue(bamFile.getCreatedDate().equals(loadBamFile.getCreatedDate()));
        Assert.isTrue(bamFile.getReferenceId().equals(loadBamFile.getReferenceId()));
        Assert.isTrue(bamFile.getPath().equals(loadBamFile.getPath()));
        Assert.isTrue(bamFile.getIndex().getPath().equals(loadBamFile.getIndex().getPath()));

        //full
        Track<Read> fullTrackQ = new Track<>();
        fullTrackQ.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
        fullTrackQ.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
        fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
        fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));
        fullTrackQ.setId(bamFile.getId());

        BamQueryOption option = new BamQueryOption();
        option.setTrackDirection(TrackDirectionType.LEFT);
        option.setShowSpliceJunction(true);
        option.setShowClipping(true);
        option.setFrame(TEST_FRAME_SIZE);
        option.setCount(TEST_COUNT);
        BamTrack<Read> fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);
        Read testRead = fullTrack.getBlocks().get(0);
        testRead(testRead);

        option.setTrackDirection(TrackDirectionType.RIGHT);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setTrackDirection(TrackDirectionType.MIDDLE);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setTrackDirection(null);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setShowSpliceJunction(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setShowSpliceJunction(null);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setShowClipping(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setShowClipping(null);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setFilterDuplicate(true);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setFilterDuplicate(false);
        option.setFilterNotPrimary(true);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setFilterNotPrimary(false);
        option.setFilterVendorQualityFail(true);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setFilterVendorQualityFail(false);
        option.setFilterSupplementaryAlignment(true);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        testBamTrack(fullTrack);

        option.setFilterSupplementaryAlignment(false);
        option.setFrame(0);
        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());

        option.setFrame(-TEST_FRAME_SIZE);
        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());

        option.setCount(LARGE_TEST_COUNT);
        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());

        option.setFrame(LARGE_FRAME_SIZE);
        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());
        option.setCount(0);

        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());

        option.setCount(null);
        option.setDownSampling(false);
        fullTrack = bamManager.getBamTrack(fullTrackQ, option);
        Assert.isTrue(fullTrack.getDownsampleCoverage().isEmpty());

        //ald method, delete next
        fullTrack = bamManager.getFullMiddleReadsResult(fullTrackQ, TEST_FRAME_SIZE, TEST_COUNT, true,
                true);

        testBamTrack(fullTrack);
        testRead = fullTrack.getBlocks().get(0);
        testRead(testRead);

        //left
        fullTrack = bamManager.getFullReadsResult(fullTrackQ, TrackDirectionType.LEFT,
                TEST_FRAME_SIZE, TEST_COUNT, false, true);

        Assert.isTrue(!fullTrack.getBlocks().isEmpty());
        testRead = fullTrack.getBlocks().get(0);
        testRead(testRead);

        fullTrack = bamManager.getFullReadsResult(fullTrackQ, TrackDirectionType.RIGHT,
                TEST_FRAME_SIZE, TEST_COUNT, true, false);

        Assert.isTrue(!fullTrack.getBlocks().isEmpty());
        testRead = fullTrack.getBlocks().get(0);
        testRead(testRead);
    }

    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void testLoadUrl() throws Exception {
        final String path = "/agnX1.09-28.trim.dm606.realign.bam";
        String bamUrl = UrlTestingUtils.TEST_FILE_SERVER_URL + path;
        String indexUrl = bamUrl + ".bai";

        Server server = new Server(UrlTestingUtils.TEST_FILE_SERVER_PORT);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                                   HttpServletResponse response) throws IOException, ServletException {
                String uri = baseRequest.getRequestURI();
                logger.info(uri);
                File file = new File(resource.getFile().getAbsolutePath() + uri);
                MultipartFileSender fileSender = MultipartFileSender.fromFile(file);
                try {
                    fileSender.with(request).with(response).serveResource();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            server.start();

            Track<Read> fullTrackQ = new Track<>();
            fullTrackQ.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
            fullTrackQ.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
            fullTrackQ.setScaleFactor(SCALE_FACTOR_SMALL);
            fullTrackQ.setChromosome(new Chromosome(testChromosome.getId()));

            BamTrack<Read> fullTrack = bamManager.getFullMiddleReadsResultFromUrl(fullTrackQ, TEST_FRAME_SIZE,
                                                                                  TEST_COUNT,
                                                              true, true, bamUrl, indexUrl);

            testBamTrack(fullTrack);
            Read testRead = fullTrack.getBlocks().get(0);
            testRead(testRead);
        } finally {
            server.stop();
        }
    }

    @Test
    @Ignore
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void s3Test() throws IOException {
        Bucket bucket = new Bucket();

        bucket.setBucketName(s3BucketName);
        bucket.setAccessKeyId(s3AccessKey);
        bucket.setSecretAccessKey(s3SecretKey);
        bucketManager.saveBucket(bucket);

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(s3FilePath);
        request.setIndexPath(s3IndexPath);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.S3);
        request.setS3BucketId(bucket.getId());
        request.setIndexS3BucketId(bucket.getId());
        request.setIndexType(BiologicalDataItemResourceType.S3);
        BamFile bamFile = bamManager.registerBam(request);
        Assert.notNull(bamFile);
        BamFile loadBamFile = bamFileManager.loadBamFile(bamFile.getId());
        Assert.notNull(loadBamFile);

        bamManager.unregisterBamFile(loadBamFile.getId());
        loadBamFile = bamFileManager.loadBamFile(bamFile.getId());
        Assert.isNull(loadBamFile);

        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(Arrays.asList(
                bamFile.getBioDataItemId(), bamFile.getIndex().getId()));
        Assert.isTrue(items.isEmpty());
    }

    @Ignore
    @Test
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void hdfsTest() throws IOException {

        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(hdfsFilePath);
        request.setIndexPath(hdfsIndexPath);
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.HDFS);

        BamFile bamFile = bamManager.registerBam(request);
        Assert.notNull(bamFile);
        BamFile loadBamFile = bamFileManager.loadBamFile(bamFile.getId());
        Assert.notNull(loadBamFile);
        bamManager.unregisterBamFile(loadBamFile.getId());
        loadBamFile = bamFileManager.loadBamFile(bamFile.getId());
        Assert.isNull(loadBamFile);

        List<BiologicalDataItem> items = biologicalDataItemDao.loadBiologicalDataItemsByIds(Arrays.asList(
                bamFile.getBioDataItemId(), bamFile.getIndex().getId()));
        Assert.isTrue(items.isEmpty());
    }

    @Test
    public void testCalculateConsensusSequencePerformance() throws IOException {
        // Set isPerfomaceTest to true if you want to measure time
        boolean isPerformanceTest = true;

        BamFile bamFile = setUpTestFile();

        int i = 0;
        double avTime1 = 0.0;
        double avTime2 = 0.0;
        double avTime3 = 0.0;
        do {
            // Small range.
            Track<Sequence> track = new Track<>();
            track.setStartIndex(TEST_START_INDEX_SMALL_RANGE);
            track.setEndIndex(TEST_END_INDEX_SMALL_RANGE);
            track.setScaleFactor(SCALE_FACTOR_SMALL);
            track.setChromosome(testChromosome);
            track.setId(bamFile.getId());

            long start = System.currentTimeMillis();
            Track<Sequence> seq = bamManager.calculateConsensusSequence(track);
            long end = System.currentTimeMillis();
            Assert.notNull(seq);
            avTime1 += (end - start);

            // Large range.
            track.setStartIndex(TEST_START_INDEX_LARGE_RANGE);
            track.setEndIndex(TEST_END_INDEX_LARGE_RANGE);
            track.setScaleFactor(SCALE_FACTOR_LARGE);

            start = System.currentTimeMillis();
            seq = bamManager.calculateConsensusSequence(track);
            end = System.currentTimeMillis();
            Assert.notNull(seq);
            avTime2 += (end - start);

            // Medium range.
            track.setStartIndex(TEST_START_INDEX_MEDIUM_RANGE);
            track.setEndIndex(TEST_END_INDEX_MEDIUM_RANGE);
            track.setScaleFactor(SCALE_FACTOR_MEDIUM);

            start = System.currentTimeMillis();
            seq = bamManager.calculateConsensusSequence(track);
            end = System.currentTimeMillis();
            Assert.notNull(seq);
            avTime3 += (end - start);

            i++;
            if (i == 10) {
                avTime1 = avTime1 / 10;
                System.out.println("BAM seq (small scale) tooks " + avTime1 + "ms");
                avTime2 = avTime2 / 10;
                System.out.println("BAM seq (large scale) tooks " + avTime2 + "ms");
                avTime3 = avTime3 / 10;
                System.out.println("BAM seq (medium scale) tooks " + avTime3 + "ms");
                isPerformanceTest = false;
            }
        } while (isPerformanceTest);
    }

    private BamFile setUpTestFile() throws IOException {
        String path = resource.getFile().getAbsolutePath() + "//agnX1.09-28.trim.dm606.realign.bam";
        IndexedFileRegistrationRequest request = new IndexedFileRegistrationRequest();
        request.setPath(path);
        request.setIndexPath(path + ".bai");
        request.setName(TEST_NSAME);
        request.setReferenceId(testReference.getId());
        request.setType(BiologicalDataItemResourceType.FILE);

        BamFile bamFile = bamManager.registerBam(request);
        Assert.notNull(bamFile);
        return bamFile;
    }

    private void testRead(Read read) {
        Assert.notNull(read);
        Assert.notNull(read.getName());
        Assert.notNull(read.getEndIndex());
        Assert.notNull(read.getStartIndex());
        Assert.notNull(read.getStand());
        Assert.notNull(read.getCigarString());
        Assert.isTrue(!read.getCigarString().isEmpty());
        Assert.notNull(read.getFlagMask());
        Assert.notNull(read.getMappingQuality());
        Assert.notNull(read.getTLen());
        Assert.notNull(read.getPNext());
        Assert.notNull(read.getRNext());
        Assert.isTrue(!read.getRNext().isEmpty());
    }

    private void testBamTrack(BamTrack<Read> fullTrack) {

        Assert.isTrue(null != fullTrack.getBlocks());
        Assert.isTrue(!fullTrack.getBlocks().isEmpty());
        Assert.isTrue(null != fullTrack.getBaseCoverage());
        Assert.isTrue(!fullTrack.getBaseCoverage().isEmpty());
        Assert.isTrue(null != fullTrack.getDownsampleCoverage());
        Assert.isTrue(!fullTrack.getDownsampleCoverage().isEmpty());
        Assert.isTrue(fullTrack.getSpliceJunctions().isEmpty());

    }
}
