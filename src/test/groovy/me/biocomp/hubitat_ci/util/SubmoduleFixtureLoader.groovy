package me.biocomp.hubitat_ci.util

/**
 * Utility class for loading submodule fixture paths from a shared configuration file.
 * This ensures all tests and CI workflows use the same list of required submodule fixtures.
 */
class SubmoduleFixtureLoader {
    
    private static final String CONFIG_FILE = ".submodule-fixtures.txt"
    
    /**
     * Loads the list of required submodule fixture paths from the shared configuration file.
     * 
     * @return List of File objects representing the required submodule fixtures
     * @throws FileNotFoundException if the configuration file does not exist
     */
    static List<File> loadSubmoduleFixtures() {
        def configFile = new File(CONFIG_FILE)
        if (!configFile.exists()) {
            throw new FileNotFoundException("Configuration file ${CONFIG_FILE} not found")
        }
        return configFile.readLines()
            .findAll { line -> line && !line.trim().startsWith('#') }
            .collect { new File(it.trim()) }
    }
}
