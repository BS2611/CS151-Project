package cs151.model;

import java.nio.file.Path;

/** The SQLite strategy ({@link SqliteDAL}) must pass the DAL contract. */
class SqliteDalTest extends DalContractTest {
    @Override
    protected DAL createDal(Path folder) {
        return new SqliteDAL(folder.resolve("test.db"));
    }
}
