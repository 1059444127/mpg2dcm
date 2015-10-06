/**
 * mpg2dcm by Tom Doel
 *
 * http://github.com/tomdoel/mpg2dcm
 *
 * Distributed under the MIT License
 */

package com.tomdoel.mpg2dcm;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parses an XML file describing an endoscopic capture that is arranged in a structured file hierarchy
 *
 * <p>Part of <a href="http://github.com/tomdoel/mpg2dcm">mpg2dcm</a>
 *
 * @author Tom Doel
 * @version 1.0
 */
public class EndoscopicFileProcessor {

    private final Attributes dicomAttributes;
    private final List<File> fullVideoFileNames = new ArrayList<>();
    private final List<File> fullPictureFileNames = new ArrayList<>();
    private final List<File> fullSoundFileNames = new ArrayList<>();

    /**
     * Creates the EndoscopicFileProcessor that will parse the XML file
     * @param xmlFile the XML file to be processed
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    public EndoscopicFileProcessor(final File xmlFile) throws ParserConfigurationException, SAXException, IOException, ParseException {

        // Parse the XML file to get the tags and video filenames
        EndoscopicXmlParser parser = parseEndoscopicXmlFile(xmlFile);

        // Convert the tags to Dicom tags
        dicomAttributes = convertEndoscopicXmlToDicomAttributes(parser.getTagMap());

        // Get full paths for each video file
        final String path = xmlFile.toPath().getParent().toString();
        fullVideoFileNames.addAll(parser.getVideoFilenames().stream().map(videoFile -> new File(path, videoFile)).collect(Collectors.toList()));
        fullPictureFileNames.addAll(parser.getPictureFilenames().stream().map(pictureFile -> new File(path, pictureFile)).collect(Collectors.toList()));
        fullSoundFileNames.addAll(parser.getSoundFilenames().stream().map(soundFile -> new File(path, soundFile)).collect(Collectors.toList()));
    }

    /**
     * @return DICOM attributes created by parsing the XML tags
     */
    public Attributes getDicomAttributes() {
        return dicomAttributes;
    }

    /**
     * @return list of video files referenced in the XML file
     */
    public List<File> getVideoFileNames() {
        return fullVideoFileNames;
    }

    /**
     * @return list of picture files referenced in the XML file
     */
    public List<File> getPictureFileNames() {
        return fullPictureFileNames;
    }

    /**
     * @return list of video files referenced in the XML file
     */
    public List<File> getSoundFileNames() {
        return fullSoundFileNames;
    }


    private EndoscopicXmlParser parseEndoscopicXmlFile(final File xmlFile) throws IOException, SAXException, ParserConfigurationException {
        final SAXParserFactory factory = SAXParserFactory.newInstance();
        final SAXParser saxParser = factory.newSAXParser();
        final EndoscopicXmlParser parser = new EndoscopicXmlParser();
        saxParser.parse(xmlFile, parser);
        return parser;
    }

    private static Attributes convertEndoscopicXmlToDicomAttributes(final Map<String, String> tagMap) throws ParseException {
        final Attributes dicomAttributes = new Attributes();
        for (final Map.Entry<String, String> entry : tagMap.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            switch (key) {
                case "PatID":
                    dicomAttributes.setString(Tag.PatientID, VR.LO, value);
                    break;
                case "PatName":
                    dicomAttributes.setString(Tag.PatientName, VR.PN, value);
                    break;
                case "ProcedureDescription":
                    dicomAttributes.setString(Tag.StudyDescription, VR.PN, value);
                    break;
                case "ProcedureID":
                    dicomAttributes.setString(Tag.StudyID, VR.PN, value);
                    break;
                case "ReferringPhysician":
                    dicomAttributes.setString(Tag.ReferringPhysicianName, VR.PN, value);
                    break;
                case "StudyInstanceUID":
                    dicomAttributes.setString(Tag.StudyInstanceUID, VR.UI, value);
                    break;
                case "SeriesInstanceUID":
                    dicomAttributes.setString(Tag.SeriesInstanceUID, VR.UI, value);
                    break;
                case "OtherPatientID":
                    dicomAttributes.setString(Tag.OtherPatientIDs, VR.UI, value);
                    break;
                case "PATAccession":
                    dicomAttributes.setString(Tag.AccessionNumber, VR.SH, value);
                    break;
                case "PATSex":
                    dicomAttributes.setString(Tag.PatientSex, VR.CS, value);
                    break;
                case "PatBirth":
                    final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                    final Date date = dateFormat.parse(value);
                    dicomAttributes.setDate(Tag.PatientBirthDate, VR.DA, date);
                    break;
                case "ORDate":
                    final DateFormat orDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ENGLISH);
                    final Date orDate = orDateFormat.parse(value);
                    dicomAttributes.setDate(Tag.StudyDate, VR.DA, orDate);
                    dicomAttributes.setDate(Tag.StudyTime, VR.DA, orDate);
                    break;
            }
        }
        return dicomAttributes;
    }
}
