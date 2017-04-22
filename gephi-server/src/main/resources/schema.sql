CREATE SCHEMA IF NOT EXISTS gephi;
CREATE TABLE IF NOT EXISTS gephi.graph (
	pk_id INT NOT NULL,
	title VARCHAR(50) NOT NULL,
	creator VARCHAR(50) DEFAULT NULL,
	directed SMALLINT NOT NULL DEFAULT 0,
	up_weight FLOAT NOT NULL DEFAULT 0,
	down_weight FLOAT NOT NULL DEFAULT 0,
	url_base VARCHAR(50) DEFAULT NULL,
	CONSTRAINT pk_graph PRIMARY KEY (pk_id)
);
CREATE TABLE IF NOT EXISTS gephi.node (
	pk_num INT NOT NULL,
	pk_graph INT NOT NULL,
	title VARCHAR(50) DEFAULT NULL,
	tag VARCHAR(200) DEFAULT NULL,
	CONSTRAINT pk_node PRIMARY KEY (pk_graph, pk_num),
	CONSTRAINT fk_node_graph FOREIGN KEY (pk_graph) REFERENCES gephi.graph (pk_id)
);
CREATE TABLE IF NOT EXISTS gephi.edge (
	pk_graph INT NOT NULL,
	pk_num INT NOT NULL,
	source_node INT NOT NULL,
	target_node INT NOT NULL,
	val FLOAT NOT NULL DEFAULT 0,
	CONSTRAINT pk_edge PRIMARY KEY (pk_graph, pk_num),
	CONSTRAINT fk_edge_graph FOREIGN KEY (pk_graph) REFERENCES gephi.graph (pk_id),
	CONSTRAINT fk_source_node FOREIGN KEY (pk_graph, source_node) REFERENCES gephi.node (pk_graph, pk_num),
	CONSTRAINT fk_target_node FOREIGN KEY (pk_graph, target_node) REFERENCES gephi.node (pk_graph, pk_num)
);