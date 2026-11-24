package cs151.model;

import java.nio.file.Path;

/** The SQLite strategy ({@link DataManager}) must pass the DAL contract. */
class SqliteDalTest extends DalContractTest {
    @Override
    protected DAL createDal(Path folder) {
        return new DataManager(folder.resolve("test.db"));
    }
}
