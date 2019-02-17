package ru.processtech.forome.annotation.database;

import com.infomaximum.database.domainobject.DomainObjectSource;
import com.infomaximum.database.maintenance.ChangeMode;
import com.infomaximum.database.maintenance.SchemaService;
import com.infomaximum.database.provider.DBProvider;
import com.infomaximum.database.schema.Schema;
import com.infomaximum.rocksdb.RocksDataBaseBuilder;
import ru.processtech.forome.annotation.Service;
import ru.processtech.forome.annotation.database.entityobject.user.UserReadable;

import java.nio.file.Path;

public class DatabaseService {

	private final Service service;

	private DomainObjectSource domainObjectSource;

	public DatabaseService(Service service) throws Exception {
		this.service = service;

		Path dataPath = service.getServiceConfig().dataPath;

		DBProvider dbProvider = new RocksDataBaseBuilder()
				.withPath(dataPath.resolve("database"))
				.build();
		this.domainObjectSource = new DomainObjectSource(dbProvider);

		Schema schema = new Schema.Builder()
				.withDomain(UserReadable.class)
				.build();

		new SchemaService(dbProvider)
				.setNamespace("ru.processtech.forome.annotation")
				.setChangeMode(ChangeMode.CREATION)
				.setSchema(schema)
				.execute();
	}

	public DomainObjectSource getDomainObjectSource() {
		return domainObjectSource;
	}
}
