
package celestibytes.gradle.util;

import org.apache.commons.io.IOUtils;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.collections.MinimalFileTree;
import org.gradle.util.GFileUtils;
import org.gradle.util.SingleMessageLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileTree implements MinimalFileTree
{
    private final File zipFile;
    
    public ZipFileTree(File zipFile)
    {
        this.zipFile = zipFile;
    }
    
    @Override
    public String getDisplayName()
    {
        return String.format("ZIP '%s'", zipFile);
    }
    
    @Override
    public void visit(FileVisitor visitor)
    {
        if (!zipFile.exists())
        {
            SingleMessageLogger.nagUserOfDeprecatedBehaviour(String.format(
                    "The specified zip file %s does not exist and will be silently ignored", getDisplayName()));
            return;
        }
        if (!zipFile.isFile())
        {
            throw new InvalidUserDataException(String.format("Cannot expand %s as it is not a file.", getDisplayName()));
        }
        
        AtomicBoolean stopFlag = new AtomicBoolean();
        
        try
        {
            ZipFile zip = new ZipFile(zipFile);
            try
            {
                // The iteration order of zip.getEntries() is based on the hash
                // of the zip entry. This isn't much use
                // to us. So, collect the entries in a map and iterate over them
                // in alphabetical order.
                Map<String, ZipEntry> entriesByName = new TreeMap<String, ZipEntry>();
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements())
                {
                    ZipEntry entry = entries.nextElement();
                    entriesByName.put(entry.getName(), entry);
                }
                Iterator<ZipEntry> sortedEntries = entriesByName.values().iterator();
                while (!stopFlag.get() && sortedEntries.hasNext())
                {
                    ZipEntry entry = sortedEntries.next();
                    if (entry.isDirectory())
                    {
                        visitor.visitDir(new DetailsImpl(entry, zip, stopFlag));
                    }
                    else
                    {
                        visitor.visitFile(new DetailsImpl(entry, zip, stopFlag));
                    }
                }
            }
            finally
            {
                zip.close();
            }
        }
        catch (Exception e)
        {
            throw new GradleException(String.format("Could not expand %s.", getDisplayName()), e);
        }
    }
    
    private class DetailsImpl implements FileVisitDetails
    {
        private final ZipEntry entry;
        private final ZipFile zip;
        private final AtomicBoolean stopFlag;
        private File file;
        
        public DetailsImpl(ZipEntry entry, ZipFile zip, AtomicBoolean stopFlag)
        {
            this.entry = entry;
            this.zip = zip;
            this.stopFlag = stopFlag;
        }
        
        public String getDisplayName()
        {
            return String.format("zip entry %s!%s", zipFile, entry.getName());
        }
        
        @Override
        public void stopVisiting()
        {
            stopFlag.set(true);
        }
        
        /**
         * Changed this to return a broken value! Be warned! Will not be a valid
         * file, do not read it. Standard Jar/Zip tasks don't care about this,
         * even though they call it.
         */
        @Override
        public File getFile()
        {
            if (file == null)
            {
                file = new File(entry.getName());
                // copyTo(file);
            }
            return file;
        }
        
        @Override
        public long getLastModified()
        {
            return entry.getTime();
        }
        
        @Override
        public boolean isDirectory()
        {
            return entry.isDirectory();
        }
        
        @Override
        public long getSize()
        {
            return entry.getSize();
        }
        
        @Override
        public InputStream open()
        {
            try
            {
                return zip.getInputStream(entry);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        
        @Override
        public RelativePath getRelativePath()
        {
            return new RelativePath(!entry.isDirectory(), entry.getName().split("/"));
        }
        
        // Stuff below this line was
        // --------------------------------------------------
        // Stolen from Gradle's
        // org.gradle.api.internal.file.AbstractFileTreeElement
        
        @Override
        public String toString()
        {
            return getDisplayName();
        }
        
        @Override
        public String getName()
        {
            return getRelativePath().getLastName();
        }
        
        @Override
        public String getPath()
        {
            return getRelativePath().getPathString();
        }
        
        @Override
        public void copyTo(OutputStream outstr)
        {
            try
            {
                InputStream inputStream = open();
                try
                {
                    IOUtils.copyLarge(inputStream, outstr);
                }
                finally
                {
                    inputStream.close();
                }
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        
        @Override
        public boolean copyTo(File target)
        {
            validateTimeStamps();
            try
            {
                if (isDirectory())
                {
                    GFileUtils.mkdirs(target);
                }
                else
                {
                    GFileUtils.mkdirs(target.getParentFile());
                    copyFile(target);
                }
                return true;
            }
            catch (Exception e)
            {
                throw new GradleException(String.format("Could not copy %s to '%s'.", new Object[] { getDisplayName(),
                        target }), e);
            }
        }
        
        private void validateTimeStamps()
        {
            long lastModified = getLastModified();
            if (lastModified < 0L)
            {
                throw new GradleException(String.format("Invalid Timestamp %s for '%s'.",
                        new Object[] { Long.valueOf(lastModified), getDisplayName() }));
            }
        }
        
        private void copyFile(File target) throws IOException
        {
            FileOutputStream outputStream = new FileOutputStream(target);
            try
            {
                copyTo(outputStream);
            }
            finally
            {
                outputStream.close();
            }
        }
        
        @Override
        public int getMode()
        {
            return isDirectory() ? 493 : 420;
        }
    }
}
