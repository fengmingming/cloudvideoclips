package boluo.repositories;

import java.io.*;

public class LocalFileInputStream extends InputStream {

    private InputStream input;
    private final File file;

    public LocalFileInputStream(File file) throws FileNotFoundException {
        this.file = file;
    }

    @Override
    public int read() throws IOException {
        if(this.input == null) {
            this.input = new FileInputStream(this.file);
        }
        return this.input.read();
    }

    public File getFile() {
        return this.file;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if(this.input != null) {
            this.input.close();
        }
    }

}
