package com.thedarshan.hqx;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import static java.util.Arrays.asList;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import joptsimple.BuiltinHelpFormatter;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

/**
 *
 * @author Darshan
 */
public class Main {

    public static final short HQ2X = 2;
    public static final short HQ3X = 3;
    public static final short HQ4X = 4;

    private static OptionParser initParser() {
        OptionParser parser = new OptionParser();
        parser.accepts("hq2x", "Upscale the input file with hq2x");
        parser.accepts("hq3x", "Upscale the input file with hq3x");
        parser.accepts("hq4x", "Upscale the input file with hq4x");
        parser.accepts("all", "Upscale the input file with hq2x, hq3x, and hq4x");
        parser.accepts("input", "Specify input image file").withRequiredArg();
        parser.accepts("output", "Override default name for output image file").withRequiredArg();
        parser.acceptsAll(asList("h", "?", "help"), "show help").forHelp();
        parser.formatHelpWith(new CustomHelpFormatter());

        return parser;
    }

    public static void showHelp(OptionParser parser){
        try {    
            parser.printHelpOn(System.out);
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        System.out.println("hqx image converter");
        System.out.println("Working Directory -> " + System.getProperty("user.dir"));

        OptionParser parser = initParser();
        OptionSet options = parser.parse(args);

        String inputFile = "";
        String outputFile = "";
        int nformat = 0;

        if (args.length == 0) {
            System.err.println("Must specify an input file");
            showHelp(parser);
            return;
        }

        if (options.has("hq2x")) {
            nformat++;
        }
        if (options.has("hq3x")) {
            nformat++;
        }
        if (options.has("hq4x")) {
            nformat++;
        }
        if (options.has("all")) {
            nformat += 2;
        }

        if (options.hasArgument("output")) {
            outputFile = (String) options.valueOf("output");
        }

        if (options.hasArgument("input")) {
            inputFile = (String) options.valueOf("input");
        } else {
            inputFile = args[args.length - 1];
        }

        if (inputFile.startsWith("-")) {
            System.err.println("Must specify an input file");
            showHelp(parser);
            return;
        }

        if (options.hasArgument("output") && nformat > 1) {
            System.err.println("Cannot specify output for multiple conversion");
            return;
        }

        if (!(options.has("hq2x") || options.has("all") || options.has("hq3x") || options.has("hq4x"))) {
            System.err.println("Must specify at least one scaling type");
            showHelp(parser);
            return;
        }

        BufferedImage inputImage;
        try {
            inputImage = ImageIO.read(new FileInputStream(inputFile));
        } catch (IOException ex) {
            System.err.println("Cannot load " + inputFile);
            return;
        }

        RgbYuv.RgbYuv_init();
        if (options.has("hq2x") || options.has("all")) {
            System.err.println("Scaling " + inputFile + " with hq2x");
            convert(inputImage, inputFile, outputFile, HQ2X);
        }
        if (options.has("hq3x") || options.has("all")) {
            System.err.println("Scaling " + inputFile + " with hq3x");
            convert(inputImage, inputFile, outputFile, HQ3X);
        }
        if (options.has("hq4x") || options.has("all")) {
            System.err.println("Scaling " + inputFile + " with hq4x");
            convert(inputImage, inputFile, outputFile, HQ4X);
        }
        RgbYuv.RgbYuv_dispose();
    }

    private static boolean convert(BufferedImage inputImage, String inputFile, String outputFile, short algo) {

        if (algo > HQ4X) {
            return false;
        }

        if (outputFile == null || outputFile.length() < 1) {
            outputFile = MessageFormat.format("{0}_hq{1}x.png", inputFile, algo);
        }

        if (inputImage == null) {
            try {
                inputImage = ImageIO.read(new FileInputStream(inputFile));
            } catch (IOException ex) {
                System.err.println("Cannot load " + inputFile);
                return false;
            }
        }
        if (inputImage != null) {
            // Convert image to ARGB if on another format
            if (inputImage.getType() != BufferedImage.TYPE_INT_ARGB && inputImage.getType() != BufferedImage.TYPE_INT_ARGB_PRE) {
                final BufferedImage temp = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                temp.getGraphics().drawImage(inputImage, 0, 0, null);
                inputImage = temp;
            }
            // Obtain pixel data for source image
            final int[] data = ((DataBufferInt) inputImage.getRaster().getDataBuffer()).getData();

            // Create the destination image, with the correct size
            final BufferedImage destinationBuffer = new BufferedImage(inputImage.getWidth() * algo, inputImage.getHeight() * algo, BufferedImage.TYPE_INT_ARGB);
            // Obtain pixel data for destination image
            final int[] dataDest = ((DataBufferInt) destinationBuffer.getRaster().getDataBuffer()).getData();
            // Resize it
            switch (algo) {
                case HQ2X:
                    Hqx_2x.hq2x_32_rb(data, dataDest, inputImage.getWidth(), inputImage.getHeight());
                    break;
                case HQ3X:
                    Hqx_3x.hq3x_32_rb(data, dataDest, inputImage.getWidth(), inputImage.getHeight());
                    break;
                case HQ4X:
                    Hqx_4x.hq4x_32_rb(data, dataDest, inputImage.getWidth(), inputImage.getHeight());
                    break;
                default:
                    return false;
            }
            try {
                // Save our result
                ImageIO.write(destinationBuffer, "PNG", new File(outputFile));
                System.out.println("Scaled image written to " + outputFile);
            } catch (IOException ex) {
                System.err.println("Cannot write image to " + outputFile);
            }
            return true;
        }
        System.err.println("Cannot convert " + inputFile);
        return false;

    }

    private static class CustomHelpFormatter implements HelpFormatter {

        private final joptsimple.BuiltinHelpFormatter baseFormatter;

        public CustomHelpFormatter() {
            baseFormatter = new BuiltinHelpFormatter(70, 5);
        }

        @Override
        public String format(Map<String, ? extends OptionDescriptor> options) {
            StringBuilder sb = new StringBuilder();
            sb.append("Usage -> hqx.jar [options] inputFile\n");
            sb.append("\t the input file can also be specified with an option \n");
            sb.append("\t If not provided, output file name will be inputfile_hq2x.png for hq2x, \n");
            sb.append("\t hq3x.png for hq3x, and so on. \n \n");
            sb.append(baseFormatter.format(options));
            return sb.toString();
        }
    }
}
