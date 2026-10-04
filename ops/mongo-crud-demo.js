// MongoDB Compass / mongosh CRUD 演示脚本。
// 该脚本只操作 sentinel_demo 数据库，不影响项目 MySQL 数据。

const demo = db.getSiblingDB('sentinel_demo');
const snapshots = demo.getCollection('server_snapshots');

snapshots.createIndex({ serverId: 1 }, { unique: true });
snapshots.createIndex({ receivedAt: -1 });

snapshots.updateOne(
  { serverId: 'demo-server-01' },
  {
    $set: {
      hostname: 'demo-host',
      platform: 'Windows',
      cpuUsagePercent: 24.6,
      memoryUsagePercent: 58.2,
      diskUsagePercent: 43.7,
      status: 'online',
      receivedAt: new Date()
    },
    $setOnInsert: { createdAt: new Date() }
  },
  { upsert: true }
);

snapshots.updateOne(
  { serverId: 'demo-server-02' },
  {
    $set: {
      hostname: 'demo-linux',
      platform: 'Linux',
      cpuUsagePercent: 71.3,
      memoryUsagePercent: 76.8,
      diskUsagePercent: 64.1,
      status: 'warning',
      receivedAt: new Date()
    },
    $setOnInsert: { createdAt: new Date() }
  },
  { upsert: true }
);

print('sentinel_demo.server_snapshots count = ' + snapshots.countDocuments());
printjson(snapshots.find({}, { _id: 0 }).sort({ serverId: 1 }).toArray());
